package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.fc.v2.service.ITPostReportRowService;

/**
 * 月度业务报表核收行 Service。从进门到落账只走 {@link #acceptBook(ReportBook)}
 * 这一个整册方法：逐行钉行号、逐行定收下/挂起、整期退回、复核不改写、回执三数
 * 同回一次对库算齐，皆出于此；不留单条增删改的把手。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TPostReportRowServiceImpl implements ITPostReportRowService {

    /** 一期行数上限，越线整期退回、一行不留 */
    private static final int MAX_ROWS = 500;

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_ACCEPTED = 1;
    private static final int STATUS_SUSPENDED = 2;

    @javax.annotation.Resource
    private TPostReportRowMapper postReportRowMapper;

    @javax.annotation.Resource
    private TPostOutletCardMapper postOutletCardMapper;

    @Override
    public TPostReportRow selectTPostReportRowById(Long id) {
        return this.postReportRowMapper.selectById(id);
    }

    /**
     * 整册核收——唯一落账通道。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportReceipt acceptBook(ReportBook book) {
        String periodNo = book == null ? null : trimToNull(book.getPeriodNo());
        String siteNo = book == null ? null : trimToNull(book.getSiteNo());
        String batchNo = book == null ? null : trimToNull(book.getBatchNo());
        List<ReportLine> incoming = book == null || book.getLines() == null
                ? new ArrayList<>() : book.getLines();

        // 空期：一行也没有，回执只写“空期”，不许报核讫
        if (incoming.isEmpty()) {
            return emptyReceipt(periodNo, batchNo);
        }

        // 表头三样不得空：期号、企业字号、批次号——否则整沓无可归处，退回
        if (periodNo == null || siteNo == null || batchNo == null) {
            ReportReceipt rejected = rejectedReceipt(periodNo, batchNo, incoming.size());
            rejected.setOutcomeText("期号、企业底档字号、批次号三者不得空，整沓退回");
            return rejected;
        }

        // 同一批次号第二次递上来：只当复核，逐行比对，不改写、不添行
        List<TPostReportRow> sameBatch = this.postReportRowMapper.selectList(
                new QueryWrapper<TPostReportRow>().eq("batch_no", batchNo).eq("del_flag", 0));
        if (sameBatch != null && !sameBatch.isEmpty()) {
            return reviewReceipt(sameBatch, incoming, batchNo);
        }

        // 期内既有行：认网点、认行号（同企业同期）
        List<TPostReportRow> existed = listPeriodStored(periodNo, siteNo);
        Set<String> existedOutlets = new HashSet<>();
        int maxRowNo = 0;
        for (TPostReportRow r : existed) {
            if (r.getOutletNo() != null) {
                existedOutlets.add(r.getOutletNo());
            }
            if (r.getRowNo() != null && r.getRowNo() > maxRowNo) {
                maxRowNo = r.getRowNo();
            }
        }

        // 同一网点同期占两行（含本沓内重复、跨沓重复）：并成一行，业务量只记一次，行次认前头
        List<ReportLine> kept = new ArrayList<>();
        Set<String> seen = new HashSet<>(existedOutlets);
        for (ReportLine line : incoming) {
            String outletNo = line == null ? null : trimToNull(line.getOutletNo());
            if (outletNo == null) {
                // 连网点都没落上的行也占一个进门序号，落挂起由前置校验去定代号
                kept.add(line);
                continue;
            }
            if (seen.add(outletNo)) {
                kept.add(line);
            }
        }

        // 越线：整期退回、一行不留（本期既有的与本沓新进的一并退净）
        if (existed.size() + kept.size() > MAX_ROWS) {
            this.postReportRowMapper.delete(new QueryWrapper<TPostReportRow>()
                    .eq("period_no", periodNo).eq("site_no", siteNo));
            return rejectedReceipt(periodNo, batchNo, existed.size() + kept.size());
        }

        // 本企业名下网点，整册查一把
        Set<String> ownOutlets = loadOwnOutlets(siteNo);

        // 进门那刻按先后钉死行序号，逐行定夺
        int rowNo = maxRowNo;
        for (ReportLine line : kept) {
            rowNo++;
            TPostReportRow row = new TPostReportRow();
            row.setBatchNo(batchNo);
            row.setPeriodNo(periodNo);
            row.setSiteNo(siteNo);
            row.setRowNo(rowNo);
            row.setDelFlag(0);
            row.setStatus(STATUS_PENDING);

            String outletNo = line == null ? null : trimToNull(line.getOutletNo());
            row.setOutletNo(outletNo);
            row.setDeliveryFreq(line == null ? null : line.getDeliveryFreq());
            BigDecimal qty = line == null ? null : line.getQty();
            row.setQty(qty);
            BigDecimal prevDiff = nz(line == null ? null : line.getPrevDiff());
            BigDecimal writeOff = nz(line == null ? null : line.getWriteOffAmt());
            row.setPrevDiff(prevDiff);
            row.setWriteOffAmt(writeOff);

            String itemCode = judgeFrontItem(outletNo, qty, prevDiff, writeOff, ownOutlets);
            if (itemCode == null) {
                row.setStatus(STATUS_ACCEPTED);
            } else {
                row.setStatus(STATUS_SUSPENDED);
                row.setItemCode(itemCode);
                row.setRemark(ReportFrontItem.reason(itemCode));
            }
            this.postReportRowMapper.insert(row);
        }

        return periodReceipt(periodNo, siteNo, ReportReceipt.BOOKED,
                "本沓已逐行落账，收下/挂起各行其位，业务量要改走差异核销一路", batchNo);
    }

    /**
     * 一行五项前置校验，按代号各挂各的；一项不格只挂本行，同册别行照常进账。
     * 顺序：网点不在名下 → 业务量空 → 业务量零或负 → 核销越过上期挂账 → 上期差异未核完。
     */
    private String judgeFrontItem(String outletNo, BigDecimal qty, BigDecimal prevDiff,
                                  BigDecimal writeOff, Set<String> ownOutlets) {
        if (outletNo == null || !ownOutlets.contains(outletNo)) {
            return ReportFrontItem.OUTLET_NOT_OWNED;
        }
        if (qty == null) {
            return ReportFrontItem.QTY_BLANK;
        }
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            return ReportFrontItem.QTY_NON_POSITIVE;
        }
        // 死线：核销数不能大过上期挂下来的数，挂三十核四十不许
        if (writeOff.compareTo(prevDiff) > 0) {
            return ReportFrontItem.WRITEOFF_EXCEEDS;
        }
        // 上期差异还没核完（核销未齐）又来报这一期
        if (prevDiff.compareTo(BigDecimal.ZERO) > 0 && writeOff.compareTo(prevDiff) < 0) {
            return ReportFrontItem.PREV_DIFF_UNSETTLED;
        }
        return null;
    }

    /** 本企业在册网点卡号集合（del_flag=0） */
    private Set<String> loadOwnOutlets(String siteNo) {
        Set<String> own = new HashSet<>();
        if (siteNo == null) {
            return own;
        }
        List<TPostOutletCard> cards = this.postOutletCardMapper.selectList(
                new QueryWrapper<TPostOutletCard>()
                        .eq("site_no", siteNo)
                        .eq("del_flag", 0)
                        .select("bill_no"));
        for (TPostOutletCard card : cards) {
            if (card.getBillNo() != null) {
                own.add(card.getBillNo());
            }
        }
        return own;
    }

    /**
     * 复核回执：同批次号再递，逐行比对，不改写任何一行落地情形，也不给本期添新行。
     */
    private ReportReceipt reviewReceipt(List<TPostReportRow> storedBatch, List<ReportLine> incoming,
                                        String batchNo) {
        storedBatch.sort(Comparator.comparing(
                r -> r.getRowNo() == null ? Integer.MAX_VALUE : r.getRowNo()));
        String periodNo = storedBatch.get(0).getPeriodNo();
        String siteNo = storedBatch.get(0).getSiteNo();

        List<ReportReceipt.ReviewDiff> diffs = new ArrayList<>();
        int n = Math.min(storedBatch.size(), incoming.size());
        for (int i = 0; i < n; i++) {
            TPostReportRow stored = storedBatch.get(i);
            ReportLine sub = incoming.get(i);
            String subOutlet = sub == null ? null : trimToNull(sub.getOutletNo());
            if (!eqStr(stored.getOutletNo(), subOutlet)) {
                diffs.add(new ReportReceipt.ReviewDiff(stored.getRowNo(), stored.getOutletNo(),
                        "outletNo", str(stored.getOutletNo()), str(subOutlet)));
            }
            if (nz(sub == null ? null : sub.getQty()).compareTo(nz(stored.getQty())) != 0) {
                diffs.add(new ReportReceipt.ReviewDiff(stored.getRowNo(), stored.getOutletNo(),
                        "qty", str(stored.getQty()), str(sub == null ? null : sub.getQty())));
            }
            if (!eqInt(stored.getDeliveryFreq(), sub == null ? null : sub.getDeliveryFreq())) {
                diffs.add(new ReportReceipt.ReviewDiff(stored.getRowNo(), stored.getOutletNo(),
                        "deliveryFreq", str(stored.getDeliveryFreq()),
                        str(sub == null ? null : sub.getDeliveryFreq())));
            }
            if (nz(sub == null ? null : sub.getPrevDiff()).compareTo(nz(stored.getPrevDiff())) != 0) {
                diffs.add(new ReportReceipt.ReviewDiff(stored.getRowNo(), stored.getOutletNo(),
                        "prevDiff", str(stored.getPrevDiff()),
                        str(sub == null ? null : sub.getPrevDiff())));
            }
            if (nz(sub == null ? null : sub.getWriteOffAmt()).compareTo(nz(stored.getWriteOffAmt())) != 0) {
                diffs.add(new ReportReceipt.ReviewDiff(stored.getRowNo(), stored.getOutletNo(),
                        "writeOffAmt", str(stored.getWriteOffAmt()),
                        str(sub == null ? null : sub.getWriteOffAmt())));
            }
        }
        if (storedBatch.size() != incoming.size()) {
            diffs.add(new ReportReceipt.ReviewDiff(null, null, "rows",
                    String.valueOf(storedBatch.size()), String.valueOf(incoming.size())));
        }

        ReportReceipt receipt = periodReceipt(periodNo, siteNo, ReportReceipt.REVIEW,
                "同批次号再递只作复核，库内一行不改写、不添行；业务量真要改，走差异核销一路", batchNo);
        receipt.setReviewDiffs(diffs);
        return receipt;
    }

    /**
     * 回执三个数对库内逐行同回一次算齐：挂起 + 已收 = 原期行数。
     */
    private ReportReceipt periodReceipt(String periodNo, String siteNo, String outcome,
                                        String outcomeText, String batchNo) {
        List<TPostReportRow> rows = listPeriodStored(periodNo, siteNo);
        ReportReceipt receipt = new ReportReceipt();
        receipt.setPeriodNo(periodNo);
        receipt.setBatchNo(batchNo);
        receipt.setOutcome(outcome);
        receipt.setOutcomeText(outcomeText);
        receipt.setTotalRows(rows.size());
        for (TPostReportRow r : rows) {
            if (r.getStatus() != null && r.getStatus() == STATUS_ACCEPTED) {
                receipt.setAcceptedRows(receipt.getAcceptedRows() + 1);
            } else if (r.getStatus() != null && r.getStatus() == STATUS_SUSPENDED) {
                receipt.setSuspendedRows(receipt.getSuspendedRows() + 1);
                receipt.getSuspended().add(new ReportReceipt.SuspendedLine(
                        r.getRowNo(), r.getOutletNo(), r.getItemCode(), r.getRemark()));
            }
        }
        // 口径自检：挂起 + 收下 = 原期行数；不平即落账有污，直接点破，不做假平
        if (receipt.getAcceptedRows() + receipt.getSuspendedRows() != receipt.getTotalRows()) {
            throw new IllegalStateException(
                    "回执口径不平：已收" + receipt.getAcceptedRows()
                            + "+挂起" + receipt.getSuspendedRows()
                            + "≠原期" + receipt.getTotalRows());
        }
        return receipt;
    }

    private ReportReceipt emptyReceipt(String periodNo, String batchNo) {
        ReportReceipt receipt = new ReportReceipt();
        receipt.setPeriodNo(periodNo);
        receipt.setBatchNo(batchNo);
        receipt.setOutcome(ReportReceipt.EMPTY);
        receipt.setOutcomeText("空期");
        return receipt;
    }

    private ReportReceipt rejectedReceipt(String periodNo, String batchNo, int wouldBe) {
        ReportReceipt receipt = new ReportReceipt();
        receipt.setPeriodNo(periodNo);
        receipt.setBatchNo(batchNo);
        receipt.setOutcome(ReportReceipt.REJECTED);
        receipt.setOutcomeText("本期行数" + wouldBe + "越过上限" + MAX_ROWS + "，整期退回、一行不留");
        return receipt;
    }

    @Override
    public List<TPostReportRow> listPeriodRows(String periodNo) {
        return listPeriodStored(periodNo, null).stream()
                .sorted(Comparator.comparing(r -> r.getRowNo() == null ? Integer.MAX_VALUE : r.getRowNo()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<TPostReportRow> listErrors(String batchNo) {
        List<TPostReportRow> rows = this.postReportRowMapper.selectList(new QueryWrapper<TPostReportRow>()
                .eq("batch_no", batchNo)
                .eq("status", STATUS_SUSPENDED)
                .eq("del_flag", 0)
                .orderByAsc("row_no"));
        return rows == null ? new ArrayList<>() : rows;
    }

    @Override
    public ReportReceipt receipt(String periodNo) {
        return periodReceipt(periodNo, null, ReportReceipt.BOOKED, "期内核收回执", null);
    }

    /**
     * 全辖对账：屏上总数同库内已收逐行加总逐企业桶比，不平当场点到行，不按比例摊。
     */
    @Override
    public ReportReconcile reconcile(String periodNo, List<ScreenCount> screen) {
        List<TPostReportRow> accepted = this.postReportRowMapper.selectList(new QueryWrapper<TPostReportRow>()
                .eq("period_no", periodNo)
                .eq("status", STATUS_ACCEPTED)
                .eq("del_flag", 0)
                .orderByAsc("site_no", "row_no"));
        if (accepted == null) {
            accepted = new ArrayList<>();
        }

        // 逐企业桶：屏上一口、库内逐行一口
        Map<String, BigDecimal> rowSumBySite = new LinkedHashMap<>();
        Map<String, List<TPostReportRow>> rowsBySite = new LinkedHashMap<>();
        for (TPostReportRow r : accepted) {
            String site = r.getSiteNo() == null ? "" : r.getSiteNo();
            rowSumBySite.merge(site, nz(r.getQty()), BigDecimal::add);
            rowsBySite.computeIfAbsent(site, k -> new ArrayList<>()).add(r);
        }
        Map<String, BigDecimal> screenBySite = new LinkedHashMap<>();
        if (screen != null) {
            for (ScreenCount c : screen) {
                if (c == null || c.getSiteNo() == null) {
                    continue;
                }
                screenBySite.merge(c.getSiteNo(), nz(c.getQty()), BigDecimal::add);
            }
        }

        ReportReconcile result = new ReportReconcile();
        result.setPeriodNo(periodNo);
        Set<String> sites = new java.util.TreeSet<>();
        sites.addAll(rowSumBySite.keySet());
        sites.addAll(screenBySite.keySet());

        BigDecimal screenTotal = BigDecimal.ZERO;
        BigDecimal rowTotal = BigDecimal.ZERO;
        boolean allBalanced = true;
        for (String site : sites) {
            BigDecimal s = nz(screenBySite.get(site));
            BigDecimal r = nz(rowSumBySite.get(site));
            screenTotal = screenTotal.add(s);
            rowTotal = rowTotal.add(r);
            ReportReconcile.BucketDiff bucket = new ReportReconcile.BucketDiff();
            bucket.setSiteNo(site);
            bucket.setScreenQty(s);
            bucket.setRowQty(r);
            boolean ok = s.compareTo(r) == 0;
            bucket.setBalanced(ok);
            if (!ok) {
                allBalanced = false;
                // 差落在哪几行当场点出来：该企业桶下已收逐行
                for (TPostReportRow row : rowsBySite.getOrDefault(site, new ArrayList<>())) {
                    ReportReconcile.RowMark mark = new ReportReconcile.RowMark(
                            row.getRowNo(), row.getSiteNo(), row.getOutletNo(), row.getQty());
                    bucket.getRows().add(mark);
                    result.getDiffRows().add(mark);
                }
            }
            result.getBuckets().add(bucket);
        }
        result.setScreenTotal(screenTotal);
        result.setRowTotal(rowTotal);
        result.setBalanced(allBalanced && screenTotal.compareTo(rowTotal) == 0);
        result.setText(result.isBalanced()
                ? "全辖屏上总数与已收逐行加总接平"
                : "全辖总数接不平，已按企业逐桶点到行，未按比例摊差");
        return result;
    }

    /**
     * 旧递法：(批次号, 逐行) 接得住，内部仍走同一个整册方法，不另开落账道。
     * 旧约：返回收下的行数；空期 0；越线整期退回 -1；重复批次不重复入库。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submitBatch(String batchNo, List<TPostReportRow> rows) {
        List<TPostReportRow> source = rows == null ? new ArrayList<>() : rows;
        if (source.isEmpty()) {
            return 0;
        }
        ReportBook book = new ReportBook();
        book.setBatchNo(batchNo);
        // 旧约一期即一批；企业字号取各行所带，行址代号缺时认旧字段 itemCode
        book.setPeriodNo(batchNo);
        String siteNo = null;
        for (TPostReportRow r : source) {
            if (r != null && trimToNull(r.getSiteNo()) != null) {
                siteNo = r.getSiteNo();
                break;
            }
        }
        book.setSiteNo(siteNo);
        List<ReportLine> lines = new ArrayList<>();
        for (TPostReportRow r : source) {
            ReportLine line = new ReportLine();
            if (r != null) {
                line.setOutletNo(r.getOutletNo() != null ? trimToNull(r.getOutletNo())
                        : trimToNull(r.getItemCode()));
                line.setQty(r.getQty());
                line.setDeliveryFreq(r.getDeliveryFreq());
                line.setPrevDiff(r.getPrevDiff());
                line.setWriteOffAmt(r.getWriteOffAmt());
            }
            lines.add(line);
        }
        book.setLines(lines);

        ReportReceipt receipt = acceptBook(book);
        if (ReportReceipt.EMPTY.equals(receipt.getOutcome())) {
            return 0;
        }
        if (ReportReceipt.REJECTED.equals(receipt.getOutcome())) {
            return -1;
        }
        return receipt.getAcceptedRows();
    }

    private List<TPostReportRow> listPeriodStored(String periodNo, String siteNo) {
        QueryWrapper<TPostReportRow> wrapper = new QueryWrapper<TPostReportRow>()
                .eq("period_no", periodNo)
                .eq("del_flag", 0);
        if (siteNo != null) {
            wrapper.eq("site_no", siteNo);
        }
        List<TPostReportRow> rows = this.postReportRowMapper.selectList(wrapper);
        return rows == null ? new ArrayList<>() : rows;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean eqStr(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean eqInt(Integer a, Integer b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
