package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.mapper.auto.TPostOutletCardMapper;
import com.fc.v2.mapper.auto.TPostReportRowMapper;
import com.fc.v2.model.auto.TPostOutletCard;
import com.fc.v2.model.auto.TPostReportRow;
import com.fc.v2.model.custom.post.ReportBook;
import com.fc.v2.model.custom.post.ReportFrontItem;
import com.fc.v2.model.custom.post.ReportLine;
import com.fc.v2.model.custom.post.ReportReceipt;
import com.fc.v2.model.custom.post.ReportReconcile;
import com.fc.v2.model.custom.post.ScreenCount;

/**
 * 整册核收规则单测：不起 Spring 容器，mapper 全 Mock，落账行收集在内存“库”里。
 *
 * @author fuce
 * @date 2026-09-29
 */
@ExtendWith(MockitoExtension.class)
class TPostReportRowServiceImplTest {

    private static final String PERIOD = "2026-09";
    private static final String SITE = "YZ00";
    private static final String BATCH = "B202609-01";

    @Mock
    private TPostReportRowMapper rowMapper;
    @Mock
    private TPostOutletCardMapper outletMapper;

    @InjectMocks
    private TPostReportRowServiceImpl service;

    /** 内存“库” */
    private final List<TPostReportRow> store = new ArrayList<>();

    @BeforeEach
    void setUp() {
        store.clear();

        // 本企业名下只有 A、B 两个网点
        lenient().when(outletMapper.selectList(any())).thenAnswer(inv -> {
            List<TPostOutletCard> cards = new ArrayList<>();
            for (String no : new String[]{"A", "B"}) {
                TPostOutletCard c = new TPostOutletCard();
                c.setBillNo(no);
                c.setSiteNo(SITE);
                cards.add(c);
            }
            return cards;
        });

        // 按 wrapper 的 SQL 段区分：批次查 / 期查 / 已收对账查
        lenient().when(rowMapper.selectList(any())).thenAnswer(inv -> {
            Wrapper<?> w = inv.getArgument(0);
            String seg = w.getSqlSegment() == null ? "" : w.getSqlSegment();
            if (seg.contains("batch_no")) {
                // 同批次号第二次递：返回库内该批行（第一次为空）
                return store.stream()
                        .filter(r -> BATCH.equals(r.getBatchNo()))
                        .collect(Collectors.toList());
            }
            if (seg.contains("status")) {
                // reconcile：只要已收行
                return store.stream()
                        .filter(r -> r.getStatus() != null && r.getStatus() == 1)
                        .collect(Collectors.toList());
            }
            return new ArrayList<>(store);
        });
        lenient().when(rowMapper.insert(any())).thenAnswer(inv -> {
            store.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(rowMapper.delete(any())).thenAnswer(inv -> {
            int n = store.size();
            store.clear();
            return n;
        });
    }

    // ---------- 造数 ----------

    private ReportBook book(String batchNo, ReportLine... lines) {
        ReportBook b = new ReportBook();
        b.setPeriodNo(PERIOD);
        b.setSiteNo(SITE);
        b.setBatchNo(batchNo);
        b.setLines(new ArrayList<>(Arrays.asList(lines)));
        return b;
    }

    private ReportLine line(String outlet, String qty, Integer freq, String prevDiff, String writeOff) {
        ReportLine l = new ReportLine();
        l.setOutletNo(outlet);
        l.setQty(qty == null ? null : new BigDecimal(qty));
        l.setDeliveryFreq(freq);
        l.setPrevDiff(prevDiff == null ? null : new BigDecimal(prevDiff));
        l.setWriteOffAmt(writeOff == null ? null : new BigDecimal(writeOff));
        return l;
    }

    private TPostReportRow stored(int rowNo, String batchNo, String outlet, String qty, int status) {
        TPostReportRow r = new TPostReportRow();
        r.setPeriodNo(PERIOD);
        r.setSiteNo(SITE);
        r.setBatchNo(batchNo);
        r.setRowNo(rowNo);
        r.setOutletNo(outlet);
        r.setQty(qty == null ? null : new BigDecimal(qty));
        r.setPrevDiff(BigDecimal.ZERO);
        r.setWriteOffAmt(BigDecimal.ZERO);
        r.setStatus(status);
        r.setDelFlag(0);
        return r;
    }

    // ---------- 正常落账：行号进门钉死 ----------

    @Test
    void accept_allGood_bookedWithPinnedRowNo() {
        ReportReceipt rc = service.acceptBook(book(BATCH,
                line("A", "10", 5, "0", "0"),
                line("B", "20", 7, "0", "0")));

        assertEquals(ReportReceipt.BOOKED, rc.getOutcome());
        assertEquals(2, rc.getTotalRows());
        assertEquals(2, rc.getAcceptedRows());
        assertEquals(0, rc.getSuspendedRows());
        // 挂起 + 已收 = 原期
        assertEquals(rc.getTotalRows(), rc.getAcceptedRows() + rc.getSuspendedRows());
        assertEquals(Integer.valueOf(1), store.get(0).getRowNo());
        assertEquals(Integer.valueOf(2), store.get(1).getRowNo());
    }

    // ---------- 一行不合格只挂那一行：五项前置代号 ----------

    @Test
    void suspend_outletNotOwned_onlyThatRowHangs() {
        ReportReceipt rc = service.acceptBook(book(BATCH,
                line("A", "10", 5, "0", "0"),
                line("X", "10", 5, "0", "0"),
                line("B", "30", 5, "0", "0")));

        assertEquals(3, rc.getTotalRows());
        assertEquals(2, rc.getAcceptedRows());
        assertEquals(1, rc.getSuspendedRows());
        assertEquals(ReportFrontItem.OUTLET_NOT_OWNED, rc.getSuspended().get(0).getItemCode());
        // 挂起笔直接报本期行序号，不靠人翻纸
        assertEquals(Integer.valueOf(2), rc.getSuspended().get(0).getRowNo());
        assertEquals(2, store.get(1).getStatus());
        assertEquals(1, store.get(0).getStatus());
        assertEquals(1, store.get(2).getStatus());
    }

    @Test
    void suspend_blankQty() {
        ReportReceipt rc = service.acceptBook(book(BATCH, line("A", null, 5, "0", "0")));
        assertEquals(ReportFrontItem.QTY_BLANK, rc.getSuspended().get(0).getItemCode());
        assertNotNull(rc.getSuspended().get(0).getReason());
    }

    @Test
    void suspend_zeroOrNegativeQty() {
        ReportReceipt rc0 = service.acceptBook(book(BATCH, line("A", "0", 5, "0", "0")));
        assertEquals(ReportFrontItem.QTY_NON_POSITIVE, rc0.getSuspended().get(0).getItemCode());

        store.clear();
        ReportReceipt rcN = service.acceptBook(book(BATCH, line("A", "-3", 5, "0", "0")));
        assertEquals(ReportFrontItem.QTY_NON_POSITIVE, rcN.getSuspended().get(0).getItemCode());
    }

    @Test
    void suspend_writeOffFortyAgainstThirty() {
        // 死线：挂三十核四十，多的十不许塞进本行
        ReportReceipt rc = service.acceptBook(book(BATCH, line("A", "10", 5, "30", "40")));
        assertEquals(1, rc.getSuspendedRows());
        assertEquals(ReportFrontItem.WRITEOFF_EXCEEDS, rc.getSuspended().get(0).getItemCode());
        assertEquals(2, store.get(0).getStatus());
    }

    @Test
    void suspend_prevDiffUnsettled() {
        // 上期差异没核完（挂30只核10）又来报本期
        ReportReceipt rc = service.acceptBook(book(BATCH, line("A", "10", 5, "30", "10")));
        assertEquals(ReportFrontItem.PREV_DIFF_UNSETTLED, rc.getSuspended().get(0).getItemCode());
    }

    @Test
    void accept_writeOffExactlyPrev_isAccepted() {
        ReportReceipt rc = service.acceptBook(book(BATCH, line("A", "10", 5, "30", "30")));
        assertEquals(1, rc.getAcceptedRows());
        assertEquals(0, rc.getSuspendedRows());
    }

    // ---------- 同网点同期两行并一行，业务量只记一次，行次认前头 ----------

    @Test
    void merge_sameOutletTwiceInOneBook_keepsFirst() {
        ReportReceipt rc = service.acceptBook(book(BATCH,
                line("A", "10", 5, "0", "0"),
                line("A", "99", 5, "0", "0"),
                line("B", "20", 5, "0", "0")));

        assertEquals(2, rc.getTotalRows());
        assertEquals(Integer.valueOf(1), store.get(0).getRowNo());
        assertEquals("10", store.get(0).getQty().toPlainString());
        assertEquals(Integer.valueOf(2), store.get(1).getRowNo());
        assertEquals("20", store.get(1).getQty().toPlainString());
    }

    @Test
    void merge_sameOutletAcrossTwoBatches_secondBookSkipsItAndRowNoContinues() {
        // 头一沓：A 在第 1 行
        store.add(stored(1, "B-1", "A", "10", 1));

        // 第二沓另起批次号：又报 A（并掉不添行），B 续行号 2
        ReportReceipt rc = service.acceptBook(book("B-2",
                line("A", "10", 5, "0", "0"),
                line("B", "20", 5, "0", "0")));

        assertEquals(2, rc.getTotalRows());
        assertEquals(1, store.size() - 1); // 第二沓只新添 B 一行
        TPostReportRow added = store.get(1);
        assertEquals("B", added.getOutletNo());
        assertEquals(Integer.valueOf(2), added.getRowNo());
        assertEquals("B-2", added.getBatchNo());
    }

    // ---------- 同批次号再递：只复核，不改写、不添行 ----------

    @Test
    void review_sameBatchSecondTime_noWriteNoInsert() {
        service.acceptBook(book(BATCH,
                line("A", "10", 5, "0", "0"),
                line("B", "20", 7, "0", "0")));
        int rowsAfterFirst = store.size();

        // 再递：业务量想改（10 -> 80），只逐行比对出来，落账一个字不动
        ReportReceipt rc = service.acceptBook(book(BATCH,
                line("A", "80", 5, "0", "0"),
                line("B", "20", 7, "0", "0")));

        assertEquals(ReportReceipt.REVIEW, rc.getOutcome());
        assertEquals(rowsAfterFirst, store.size());
        assertEquals("10", store.get(0).getQty().toPlainString());
        assertTrue(rc.getReviewDiffs().stream().anyMatch(d -> "qty".equals(d.getField())));
        verify(rowMapper, never()).update(any(), any());
        verify(rowMapper, never()).updateById(any());
    }

    // ---------- 越线整期退回，一行不留 ----------

    @Test
    void reject_overLimitWholePeriodReturnsNothing() {
        ReportLine[] many = new ReportLine[501];
        for (int i = 0; i < many.length; i++) {
            // 每行换网点（不在名下也无妨——越线判定先于落账，整期照样退回）
            many[i] = line("N" + i, "1", 5, "0", "0");
        }
        ReportReceipt rc = service.acceptBook(book(BATCH, many));

        assertEquals(ReportReceipt.REJECTED, rc.getOutcome());
        assertTrue(store.isEmpty());
        verify(rowMapper, never()).insert(any());
    }

    // ---------- 空期：只写空期，不许报核讫 ----------

    @Test
    void emptyBook_onlyEmptyWord() {
        ReportReceipt rc = service.acceptBook(book(BATCH));
        assertEquals(ReportReceipt.EMPTY, rc.getOutcome());
        assertEquals("空期", rc.getOutcomeText());
        assertEquals(0, rc.getTotalRows());
        assertEquals(0, rc.getAcceptedRows());
        assertEquals(0, rc.getSuspendedRows());
        verify(rowMapper, never()).insert(any());
    }

    // ---------- 旧递法兼容 ----------

    @Test
    void legacySubmitBatch_returnsAcceptedCount() {
        TPostReportRow r1 = new TPostReportRow();
        r1.setSiteNo(SITE);
        r1.setOutletNo("A");
        r1.setQty(new BigDecimal("10"));
        TPostReportRow r2 = new TPostReportRow();
        r2.setSiteNo(SITE);
        r2.setOutletNo("B");
        r2.setQty(new BigDecimal("20"));

        int accepted = service.submitBatch(BATCH, Arrays.asList(r1, r2));
        assertEquals(2, accepted);
    }

    @Test
    void legacySubmitBatch_emptyZero_overLimitMinusOne_duplicateNoReinsert() {
        assertEquals(0, service.submitBatch(BATCH, new ArrayList<>()));

        List<TPostReportRow> many = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            TPostReportRow r = new TPostReportRow();
            r.setSiteNo(SITE);
            r.setOutletNo("N" + i);
            r.setQty(new BigDecimal("1"));
            many.add(r);
        }
        assertEquals(-1, service.submitBatch("BIG", many));

        // 重复批次：只复核，不再添行，返回库内收下数
        store.add(stored(1, "DUP", "A", "10", 1));
        TPostReportRow again = new TPostReportRow();
        again.setSiteNo(SITE);
        again.setOutletNo("A");
        again.setQty(new BigDecimal("10"));
        int sizeBefore = store.size();
        int ret = service.submitBatch("DUP", Arrays.asList(again));
        assertEquals(1, ret);
        assertEquals(sizeBefore, store.size());
    }

    // ---------- 全辖对账：不平逐企业点到行，不按比例摊 ----------

    @Test
    void reconcile_unbalanced_pointsRowsInsteadOfSpreading() {
        store.add(stored(1, BATCH, "A", "50", 1));
        store.add(stored(2, BATCH, "B", "70", 1));
        // 挂起行不进已收加总
        TPostReportRow hang = stored(3, BATCH, "A", "999", 2);
        store.add(hang);

        ReportReconcile rc = service.reconcile(PERIOD,
                Arrays.asList(new ScreenCount(SITE, new BigDecimal("100"))));

        assertFalse(rc.isBalanced());
        assertEquals(new BigDecimal("100"), rc.getScreenTotal());
        assertEquals(new BigDecimal("120"), rc.getRowTotal());
        assertEquals(1, rc.getBuckets().size());
        assertFalse(rc.getBuckets().get(0).isBalanced());
        // 差落在哪几行当场点出（行序号 1、2），不是拿 20 按比例摊全期
        List<Integer> rowNos = rc.getDiffRows().stream()
                .map(ReportReconcile.RowMark::getRowNo).sorted().collect(Collectors.toList());
        assertEquals(Arrays.asList(1, 2), rowNos);
    }

    @Test
    void reconcile_balanced() {
        store.add(stored(1, BATCH, "A", "50", 1));
        store.add(stored(2, BATCH, "B", "70", 1));

        ReportReconcile rc = service.reconcile(PERIOD,
                Arrays.asList(new ScreenCount(SITE, new BigDecimal("120"))));
        assertTrue(rc.isBalanced());
        assertTrue(rc.getDiffRows().isEmpty());
    }
}
