package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TPostCompanyArchMapper;
import com.fc.v2.mapper.auto.TPostOutletCardMapper;
import com.fc.v2.mapper.auto.TPostReportReceiptMapper;
import com.fc.v2.mapper.auto.TPostReportRowMapper;
import com.fc.v2.model.auto.TPostOutletCard;
import com.fc.v2.model.auto.TPostReportReceipt;
import com.fc.v2.model.auto.TPostReportRow;
import com.fc.v2.model.custom.ReportBook;
import com.fc.v2.model.custom.ReportReconcile;
import com.fc.v2.service.impl.TPostReportRowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 月度业务报表整册处理规则单测：mock 四个 Mapper，内存里记账，
 * 逐行挂起、核销死线、合并、行次、复核、超限整退、空期、回执口径、对账点行全覆盖。
 */
@ExtendWith(MockitoExtension.class)
class TPostReportRowServiceImplTest {

    @Mock
    private TPostReportRowMapper rowMapper;
    @Mock
    private TPostReportReceiptMapper receiptMapper;
    @Mock
    private TPostOutletCardMapper outletCardMapper;
    @Mock
    private TPostCompanyArchMapper companyArchMapper;

    @InjectMocks
    private TPostReportRowServiceImpl service;

    private final List<TPostReportRow> rowStore = new ArrayList<>();
    private final List<TPostReportReceipt> receiptStore = new ArrayList<>();
    private final Set<String> ownedSites = new HashSet<>(Arrays.asList(
            "WD_OK", "WD_A", "WD_B", "WD_C", "WD_D"));
    private long idSeq = 1;

    @BeforeEach
    void setUp() {
        rowStore.clear();
        receiptStore.clear();

        lenient().when(rowMapper.insert(any())).thenAnswer(inv -> {
            TPostReportRow r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(Long.valueOf(idSeq++));
            }
            rowStore.add(r);
            return 1;
        });
        lenient().when(receiptMapper.insert(any())).thenAnswer(inv -> {
            TPostReportReceipt r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(Long.valueOf(idSeq++));
            }
            receiptStore.add(r);
            return 1;
        });
        lenient().when(rowMapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<?> w = inv.getArgument(0);
            String seg = w.getSqlSegment();
            Object[] params = w.getParamNameValuePairs().values().toArray();
            String str = null;
            Integer intVal = null;
            for (Object v : params) {
                if (v instanceof String && str == null) {
                    str = (String) v;
                } else if (v instanceof Integer) {
                    intVal = (Integer) v;
                }
            }
            List<TPostReportRow> all = new ArrayList<>();
            for (TPostReportRow r : rowStore) {
                if (r.getDelFlag() == null || r.getDelFlag() == 0) {
                    all.add(r);
                }
            }
            List<TPostReportRow> out = new ArrayList<>();
            for (TPostReportRow r : all) {
                if (seg != null && seg.contains("batch_no") && (str == null || !str.equals(r.getBatchNo()))) {
                    continue;
                }
                if (seg != null && seg.contains("period") && (str == null || !str.equals(r.getPeriod()))) {
                    continue;
                }
                if (seg != null && seg.contains("status") && (intVal == null || !intVal.equals(r.getStatus()))) {
                    continue;
                }
                out.add(r);
            }
            return out;
        });
        lenient().when(receiptMapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<?> w = inv.getArgument(0);
            String seg = w.getSqlSegment();
            String s = stringParam(w);
            if (seg != null && seg.contains("period")) {
                List<TPostReportReceipt> out = new ArrayList<>();
                for (TPostReportReceipt r : receiptStore) {
                    if ((r.getDelFlag() == null || r.getDelFlag() == 0) && s != null && s.equals(r.getPeriod())) {
                        out.add(r);
                    }
                }
                return out;
            }
            return new ArrayList<>(receiptStore);
        });
        lenient().when(receiptMapper.selectOne(any())).thenAnswer(inv -> {
            QueryWrapper<?> w = inv.getArgument(0);
            String s = stringParam(w);
            for (TPostReportReceipt r : receiptStore) {
                if ((r.getDelFlag() == null || r.getDelFlag() == 0) && s != null && s.equals(r.getBatchNo())) {
                    return r;
                }
            }
            return null;
        });
        lenient().when(companyArchMapper.selectCount(any())).thenReturn(1);
        // 旧入参兜底：按网点卡号反查到卡，卡挂在册企业 YZ00 下
        lenient().when(outletCardMapper.selectOne(any())).thenAnswer(inv -> {
            QueryWrapper<?> w = inv.getArgument(0);
            w.getSqlSegment();
            for (Object v : w.getParamNameValuePairs().values()) {
                if (v instanceof String && ownedSites.contains(v)) {
                    TPostOutletCard card = new TPostOutletCard();
                    card.setBillNo((String) v);
                    card.setSiteNo("YZ00");
                    return card;
                }
            }
            return null;
        });
        lenient().when(outletCardMapper.selectCount(any())).thenAnswer(inv -> {
            QueryWrapper<?> w = inv.getArgument(0);
            w.getSqlSegment(); // MP 3.4.2 的具名参数在渲染 segment 时才填入 map
            for (Object v : w.getParamNameValuePairs().values()) {
                if (v instanceof String && ownedSites.contains(v)) {
                    return 1;
                }
            }
            return 0;
        });    }

    private static String stringParam(QueryWrapper<?> w) {
        w.getSqlSegment(); // MP 3.4.2 先渲染，具名参数才进 map
        for (Object v : w.getParamNameValuePairs().values()) {
            if (v instanceof String) {
                return (String) v;
            }
        }
        return null;
    }

    private static TPostReportRow row(String site, String qty, String prev, String off) {
        TPostReportRow r = new TPostReportRow();
        r.setSiteNo(site);
        if (qty != null) {
            r.setQty(new BigDecimal(qty));
        }
        if (prev != null) {
            r.setPrevDiff(new BigDecimal(prev));
        }
        if (off != null) {
            r.setWriteOff(new BigDecimal(off));
        }
        r.setFreq(2);
        return r;
    }

    private ReportBook book(String batchNo, String period, TPostReportRow... rows) {
        ReportBook b = new ReportBook();
        b.setBatchNo(batchNo);
        b.setPeriod(period);
        b.setCompanySiteNo("YZ00");
        List<TPostReportRow> list = new ArrayList<>();
        Collections.addAll(list, rows);
        b.setRows(list);
        return b;
    }

    // 1. 一行不合格只挂那一行，五类前置项各归各，别行照常进账
    @Test
    void testSuspendEachBadRowIndependently() {
        TPostReportReceipt rc = service.submitBatch(book("B1", "2026-09",
                row("WD_OK", "10", "3", "3"),      // 已收：网点在册、量正、核销不超额
                row("WD_A", null, null, null),     // QTY_EMPTY
                row("WD_B", "0", null, null),      // QTY_NONPOSITIVE
                row("WD_C", "5", "3", null),       // PREV_DIFF_OPEN：挂着差异一分不核
                row("WD_D", "8", "30", "40"),      // WRITEOFF_EXCEED：挂三十核四十
                row("WD_OTHER", "9", null, null)));// OUTLET_NOT_OWNED

        assertEquals(1, rc.getOutcome().intValue());
        assertEquals(6, rc.getTotalRows().intValue());
        assertEquals(1, rc.getAcceptedRows().intValue());
        assertEquals(5, rc.getSuspendedRows().intValue());
        // 收下 + 挂起 = 原期行数
        assertEquals(rc.getTotalRows().intValue(),
                rc.getAcceptedRows() + rc.getSuspendedRows());
        assertEquals(0, new BigDecimal("10").compareTo(rc.getAcceptedQty()));
        assertEquals("核讫", rc.getMessage());

        Map<String, String> codeBySite = new java.util.HashMap<>();
        for (TPostReportRow r : service.listRows("B1")) {
            if (r.getStatus() == 2) {
                codeBySite.put(r.getSiteNo() + "#" + r.getRowNo(), r.getItemCode());
            }
        }
        assertEquals("QTY_EMPTY", codeBySite.get("WD_A#2"));
        assertEquals("QTY_NONPOSITIVE", codeBySite.get("WD_B#3"));
        assertEquals("PREV_DIFF_OPEN", codeBySite.get("WD_C#4"));
        assertEquals("WRITEOFF_EXCEED", codeBySite.get("WD_D#5"));
        assertEquals("OUTLET_NOT_OWNED", codeBySite.get("WD_OTHER#6"));
        // 挂起行都带一句话缘故，多出的核销不许顶邻行
        for (TPostReportRow r : service.listErrors("B1")) {
            assertNotNull(r.getRemark());
            assertFalse(r.getRemark().trim().isEmpty());
        }
    }

    // 2. 进门行次那一刻钉死，翻挂起直接报行序号
    @Test
    void testRowNoPinnedAtDoor() {
        service.submitBatch(book("B2", "2026-09",
                row("WD_OK", "1", null, null),
                row("WD_BAD", "5", null, null),   // 挂起
                row("WD_A", "2", null, null)));
        List<TPostReportRow> rows = service.listRows("B2");
        assertEquals(3, rows.size());
        assertEquals(1, rows.get(0).getRowNo().intValue());
        assertEquals(2, rows.get(1).getRowNo().intValue());
        assertEquals(3, rows.get(2).getRowNo().intValue());
        assertEquals(2, rows.get(1).getStatus().intValue());
        assertEquals("OUTLET_NOT_OWNED", rows.get(1).getItemCode());
    }

    // 3. 同一网点同一期占两行：并作一行、量记一次、行次认前头；跨批次（多沓）同期也并
    @Test
    void testDuplicateSiteMergedKeepFirst() {
        TPostReportReceipt rc1 = service.submitBatch(book("B3", "2026-09",
                row("WD_OK", "10", null, null),
                row("WD_A", "4", null, null)));
        assertEquals(2, rc1.getTotalRows().intValue());

        TPostReportRow dup1 = row("WD_OK", "99", null, null); // 同册重复
        TPostReportRow dup2 = row("WD_A", "88", null, null);  // 本期他沓已落，同网点
        TPostReportRow fresh = row("WD_B", "7", null, null);
        TPostReportReceipt rc2 = service.submitBatch(book("B4", "2026-09", dup1, dup2, fresh));

        assertEquals(1, rc2.getTotalRows().intValue());
        assertEquals(2, rc2.getMergedRows().intValue());
        List<TPostReportRow> rows = service.listRows("B4");
        assertEquals(1, rows.size());
        assertEquals("WD_B", rows.get(0).getSiteNo());
        // 行次认进门先后：这一沓里它排第三，仍钉 3
        assertEquals(3, rows.get(0).getRowNo().intValue());
        // 头一沓业务量只记一次，重复的 99/88 没塞进去
        assertEquals(0, new BigDecimal("10").compareTo(
                service.listRows("B3").get(0).getQty()));
    }

    // 4. 同一批次号第二次递入只复核：不改账、不添行；要改写走差异那一路
    @Test
    void testSecondSubmissionIsReviewOnly() {
        service.submitBatch(book("B5", "2026-09",
                row("WD_OK", "10", null, null),
                row("WD_A", "4", null, null)));
        int rowsAfterFirst = service.listRows("B5").size();

        // 逐行一致
        TPostReportReceipt same = service.submitBatch(book("B5", "2026-09",
                row("WD_OK", "10", null, null),
                row("WD_A", "4", null, null)));
        assertEquals(4, same.getOutcome().intValue());
        assertTrue(same.getMessage().contains("复核一致"));
        assertEquals(rowsAfterFirst, service.listRows("B5").size());
        assertEquals(1, receiptStore.size(), "复核不得再落回执");

        // 想改业务量：只点出对不上，仍不改账不添行
        TPostReportReceipt changed = service.submitBatch(book("B5", "2026-09",
                row("WD_OK", "11", null, null),
                row("WD_A", "4", null, null)));
        assertEquals(4, changed.getOutcome().intValue());
        assertTrue(changed.getMessage().contains("对不上"));
        assertEquals(rowsAfterFirst, service.listRows("B5").size());
        assertEquals(0, new BigDecimal("10").compareTo(
                service.listRows("B5").get(0).getQty()), "库里仍是原数");
    }

    // 4b. 首册里被并掉的重复行占进门序号但没落库；原样再递，复核仍一致、不误报
    @Test
    void testReviewWithMergedRowStillConsistent() {
        service.submitBatch(book("B5B", "2026-07",
                row("WD_OK", "10", null, null),
                row("WD_OK", "99", null, null),  // 同册重复，进门第2，被并掉
                row("WD_A", "4", null, null)));  // 进门第3，落库行次 3

        TPostReportReceipt view = service.submitBatch(book("B5B", "2026-07",
                row("WD_OK", "10", null, null),
                row("WD_OK", "99", null, null),
                row("WD_A", "4", null, null)));
        assertEquals(4, view.getOutcome().intValue());
        assertTrue(view.getMessage().contains("复核一致"), view.getMessage());
    }

    // 5. 越上限整期退回、一行不留
    @Test
    void testOverLimitRejectsWholePeriod() {
        TPostReportRow[] many = new TPostReportRow[501];
        for (int i = 0; i < many.length; i++) {
            many[i] = row("WD_OK", "1", null, null);
        }
        TPostReportReceipt rc = service.submitBatch(book("B6", "2026-09", many));
        assertEquals(3, rc.getOutcome().intValue());
        assertEquals(0, rc.getTotalRows().intValue());
        assertTrue(rowStore.isEmpty());
        assertTrue(receiptStore.isEmpty(), "整期退回不留回执");
        verify(rowMapper, never()).insert(any());
    }

    // 6. 空期只写“空期”，不许报核讫
    @Test
    void testEmptyPeriodNotCleared() {
        TPostReportReceipt rc = service.submitBatch(book("B7", "2026-09"));
        assertEquals(2, rc.getOutcome().intValue());
        assertEquals("空期", rc.getMessage());
        assertEquals(0, rc.getTotalRows().intValue());
        assertEquals(0, rc.getAcceptedRows().intValue());
        assertEquals(0, rc.getSuspendedRows().intValue());
        assertTrue(rowStore.isEmpty());
        assertEquals(1, receiptStore.size());

        // 旧签名：空期 0、超限 -1、正常返已收行数
        assertEquals(0, service.submitBatch("B8", new ArrayList<TPostReportRow>()));
        TPostReportRow[] many = new TPostReportRow[501];
        Arrays.setAll(many, i -> row("WD_OK", "1", null, null));
        assertEquals(-1, service.submitBatch("B9", Arrays.asList(many)));
        TPostReportRow good = row("WD_OK", "3", null, null);
        good.setCompanySiteNo("YZ00");
        TPostReportRow badSite = row("WD_OTHER", "1", null, null);
        badSite.setCompanySiteNo("YZ00");
        assertEquals(1, service.submitBatch("B10", Arrays.asList(good, badSite)));
    }

    // 7. 全辖对账：平就接平；收后被动过就点出差在那几行，不按比例摊
    @Test
    void testReconcilePointsMismatchedRows() {
        service.submitBatch(book("B11", "2026-09",
                row("WD_OK", "10", null, null),
                row("WD_A", "5", null, null)));
        ReportReconcile ok = service.reconcile("2026-09");
        assertTrue(ok.isBalanced());
        assertEquals(0, ok.getRowQty().compareTo(new BigDecimal("15")), "逐行业务量合计 10+5");
        assertEquals(0, ok.getDiffQty().compareTo(BigDecimal.ZERO), "接平时差量为 0");

        // 模拟老三案：收进去之后库里某行被人动了
        for (TPostReportRow r : rowStore) {
            if ("WD_OK".equals(r.getSiteNo())) {
                r.setQty(new BigDecimal("13"));
            }
        }
        ReportReconcile bad = service.reconcile("2026-09");
        assertFalse(bad.isBalanced());
        assertEquals(0, bad.getDiffQty().compareTo(new BigDecimal("3")));
        assertFalse(bad.getMismatchedRows().isEmpty());
        boolean pointed = false;
        for (TPostReportRow r : bad.getMismatchedRows()) {
            if ("WD_OK".equals(r.getSiteNo())) {
                pointed = true;
            }
        }
        assertTrue(pointed, "差在哪行当场点出哪行");
        assertFalse(bad.getMismatchedReceipts().isEmpty());
    }

    // 8. 有逐行落账却没回执（收的时候/收之后口径断了），对账也点出来
    @Test
    void testReconcileMissingReceipt() {
        service.submitBatch(book("B12", "2026-08", row("WD_OK", "2", null, null)));
        receiptStore.clear();
        ReportReconcile bad = service.reconcile("2026-08");
        assertFalse(bad.isBalanced());
        assertFalse(bad.getMismatchedRows().isEmpty());
    }
}
