package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TPostCompanyArchMapper;
import com.fc.v2.mapper.auto.TPostOutletCardMapper;
import com.fc.v2.mapper.auto.TPostReportReceiptMapper;
import com.fc.v2.mapper.auto.TPostReportRowMapper;
import com.fc.v2.model.auto.TPostCompanyArch;
import com.fc.v2.model.auto.TPostOutletCard;
import com.fc.v2.model.auto.TPostReportReceipt;
import com.fc.v2.model.auto.TPostReportRow;
import com.fc.v2.model.custom.ReportBook;
import com.fc.v2.model.custom.ReportReconcile;
import com.fc.v2.service.ITPostReportRowService;

/**
 * 月度业务报表核收行 Service业务层处理。
 *
 * 进门到落账只走 {@link #submitBatch(ReportBook)} 这一条道：
 * 逐行收下/挂起、整期退回、回执三数、空期与复核，全在这一个方法里定夺。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TPostReportRowServiceImpl implements ITPostReportRowService {

    /** 单期行数上限，越线整期退回、一行不留 */
    private static final int MAX_ROWS = 500;

    /** 行落地情形 0待核 1已收 2挂起 */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_ACCEPTED = 1;
    private static final int STATUS_SUSPENDED = 2;

    /** 回执情形 1核收 2空期 3整期退回 4复核 */
    private static final int OUTCOME_ACCEPTED = 1;
    private static final int OUTCOME_EMPTY = 2;
    private static final int OUTCOME_REJECTED = 3;
    private static final int OUTCOME_REVIEW = 4;

    /** 挂起前置项代号，各按各的前置项落档 */
    public static final String ITEM_OUTLET_NOT_OWNED = "OUTLET_NOT_OWNED";
    public static final String ITEM_QTY_EMPTY = "QTY_EMPTY";
    public static final String ITEM_QTY_NONPOSITIVE = "QTY_NONPOSITIVE";
    public static final String ITEM_PREV_DIFF_OPEN = "PREV_DIFF_OPEN";
    public static final String ITEM_WRITEOFF_EXCEED = "WRITEOFF_EXCEED";

    @javax.annotation.Resource
    private TPostReportRowMapper postReportRowMapper;

    @javax.annotation.Resource
    private TPostReportReceiptMapper postReportReceiptMapper;

    @javax.annotation.Resource
    private TPostOutletCardMapper postOutletCardMapper;

    @javax.annotation.Resource
    private TPostCompanyArchMapper postCompanyArchMapper;

    @Override
    public TPostReportRow selectTPostReportRowById(Long id) {
        return this.postReportRowMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submitBatch(String batchNo, List<TPostReportRow> rows) {
        TPostReportReceipt receipt = submitBatch(ReportBook.of(batchNo, rows));
        // 旧口径：整期退回 -1、空期 0、正常返回已收行数；复核不改账，返回库内原已收行数
        if (receipt.getOutcome() != null && receipt.getOutcome() == OUTCOME_REJECTED) {
            return -1;
        }
        return receipt.getAcceptedRows() == null ? 0 : receipt.getAcceptedRows().intValue();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TPostReportReceipt submitBatch(ReportBook book) {
        if (book == null || isBlank(book.getBatchNo())) {
            return receipt(null, null, OUTCOME_REJECTED, "批次号为空，整期退回，一行不留", 0, 0, 0, 0, null);
        }
        String batchNo = book.getBatchNo().trim();
        String period = book.getPeriod() == null ? null : book.getPeriod().trim();
        String companySiteNo = book.getCompanySiteNo() == null ? null : book.getCompanySiteNo().trim();
        List<TPostReportRow> incoming = book.getRows() == null ? new ArrayList<TPostReportRow>() : book.getRows();

        // 同一批次号第二次递上来：只当复核——不改写任何一行落地情形，也不给本期添新行
        TPostReportReceipt existing = this.postReportReceiptMapper.selectOne(
                new QueryWrapper<TPostReportReceipt>().eq("batch_no", batchNo).eq("del_flag", 0));
        if (existing != null) {
            return review(batchNo, existing, incoming);
        }

        // 一行也没有：回执只写“空期”，不许报核讫
        List<TPostReportRow> rows = new ArrayList<TPostReportRow>();
        for (TPostReportRow r : incoming) {
            if (r != null) {
                rows.add(r);
            }
        }
        if (rows.isEmpty()) {
            TPostReportReceipt receipt = receipt(batchNo, period, OUTCOME_EMPTY, "空期", 0, 0, 0, 0, BigDecimal.ZERO);
            persistReceipt(receipt);
            return receipt;
        }

        // 越线整期退回、一行不留（先于任何落库判断）
        if (rows.size() > MAX_ROWS) {
            return receipt(batchNo, period, OUTCOME_REJECTED,
                    "本期" + rows.size() + "行，越过单期上限" + MAX_ROWS + "行，整期退回，一行不留",
                    0, 0, 0, 0, null);
        }

        // 报送企业须在册
        boolean companyActive = false;
        if (!isBlank(companySiteNo)) {
            Integer c = this.postCompanyArchMapper.selectCount(new QueryWrapper<TPostCompanyArch>()
                    .eq("site_no", companySiteNo).eq("status", 0).eq("del_flag", 0));
            companyActive = c != null && c.intValue() > 0;
        }

        // 本期已在他沓占过行的网点：同一期同一网点只认进门前头那一行，业务量只记一次。
        // 各行仍各带各的批次号（休把这一沓的行塞进那一沓），重复行只并不落。
        Set<String> periodSites = new HashSet<String>();
        if (!isBlank(period)) {
            List<TPostReportRow> landed = this.postReportRowMapper.selectList(new QueryWrapper<TPostReportRow>()
                    .eq("period", period).eq("del_flag", 0));
            for (TPostReportRow r : landed) {
                if (r.getSiteNo() != null) {
                    periodSites.add(r.getSiteNo());
                }
            }
        }

        int merged = 0;
        Set<String> seenThisBook = new HashSet<String>();
        int seq = 0;
        List<TPostReportRow> toInsert = new ArrayList<TPostReportRow>();
        for (TPostReportRow r : rows) {
            seq++; // 进门行次这一刻钉死，日后翻挂起直接报这个序号，不靠人对着纸数
            String siteNo = r.getSiteNo() == null ? null : r.getSiteNo().trim();

            if (!isBlank(siteNo) && (seenThisBook.contains(siteNo) || periodSites.contains(siteNo))) {
                // 同一网点同一期占两行：并作一行，业务量只记一次，行次认前头那一个
                merged++;
                continue;
            }
            if (!isBlank(siteNo)) {
                seenThisBook.add(siteNo);
            }

            r.setPeriod(period);
            r.setBatchNo(batchNo);
            r.setRowNo(Integer.valueOf(seq));
            r.setDelFlag(0);
            r.setItemCode(null);
            r.setRemark(null);

            String itemCode = checkRow(r, siteNo, companySiteNo, companyActive);
            if (itemCode == null) {
                r.setStatus(Integer.valueOf(STATUS_ACCEPTED));
            } else {
                r.setStatus(Integer.valueOf(STATUS_SUSPENDED));
                r.setItemCode(itemCode);
                r.setRemark(reasonOf(itemCode));
            }
            toInsert.add(r);
        }

        // 逐行落账——一行不合格只挂那一行，别行照常进账
        for (TPostReportRow r : toInsert) {
            this.postReportRowMapper.insert(r);
        }

        // 回执三数：落账后按库里逐行同一回计算，界面自数不作数
        TPostReportReceipt receipt = countReceipt(batchNo, period, merged);
        persistReceipt(receipt);
        return receipt;
    }

    /** 逐行前置项校验，返回挂起项代号；null 表示收下。检查顺序即前置项先后。 */
    private String checkRow(TPostReportRow r, String siteNo, String companySiteNo, boolean companyActive) {
        if (isBlank(siteNo)) {
            return ITEM_OUTLET_NOT_OWNED;
        }
        if (!isBlank(companySiteNo)) {
            if (!companyActive) {
                return ITEM_OUTLET_NOT_OWNED;
            }
            Integer owned = this.postOutletCardMapper.selectCount(new QueryWrapper<TPostOutletCard>()
                    .eq("bill_no", siteNo).eq("site_no", companySiteNo).eq("del_flag", 0));
            if (owned == null || owned.intValue() == 0) {
                return ITEM_OUTLET_NOT_OWNED;
            }
        } else {
            // 旧递法没随报送企业：按网点卡号反查，卡不存在或所属企业已注销，照样不在可报名下
            TPostOutletCard card = this.postOutletCardMapper.selectOne(new QueryWrapper<TPostOutletCard>()
                    .eq("bill_no", siteNo).eq("del_flag", 0));
            if (card == null || isBlank(card.getSiteNo())) {
                return ITEM_OUTLET_NOT_OWNED;
            }
            Integer active = this.postCompanyArchMapper.selectCount(new QueryWrapper<TPostCompanyArch>()
                    .eq("site_no", card.getSiteNo()).eq("status", 0).eq("del_flag", 0));
            if (active == null || active.intValue() == 0) {
                return ITEM_OUTLET_NOT_OWNED;
            }
        }
        if (r.getQty() == null) {
            return ITEM_QTY_EMPTY;
        }
        if (r.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            return ITEM_QTY_NONPOSITIVE;
        }
        BigDecimal prev = r.getPrevDiff();
        BigDecimal off = r.getWriteOff();
        // 上期差异还没核完又来报这一期：挂着笔差异却一分不核
        if (prev != null && prev.compareTo(BigDecimal.ZERO) > 0
                && (off == null || off.compareTo(BigDecimal.ZERO) <= 0)) {
            return ITEM_PREV_DIFF_OPEN;
        }
        // 死线：核销数不能大过上期挂下数；没有挂下差异却报核销，同样不许塞进这一行
        if (off != null && off.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal base = prev == null ? BigDecimal.ZERO : prev;
            if (off.compareTo(base) > 0) {
                return ITEM_WRITEOFF_EXCEED;
            }
        }
        return null;
    }

    /** 同批次号复核：逐行比对，不改账、不添行 */
    private TPostReportReceipt review(String batchNo, TPostReportReceipt stored, List<TPostReportRow> incoming) {
        List<TPostReportRow> landed = listRows(batchNo);
        Map<Integer, TPostReportRow> landedByRowNo = new HashMap<Integer, TPostReportRow>();
        for (TPostReportRow l : landed) {
            landedByRowNo.put(l.getRowNo(), l);
        }
        // 还原首册进门视角：本期他沓已占的、本册重复的网点，进门时就是被并掉的行，不参与逐行比对
        Set<String> periodSites = new HashSet<String>();
        if (!isBlank(stored.getPeriod())) {
            for (TPostReportRow r : this.postReportRowMapper.selectList(new QueryWrapper<TPostReportRow>()
                    .eq("period", stored.getPeriod()).eq("del_flag", 0))) {
                if (r.getSiteNo() != null && !batchNo.equals(r.getBatchNo())) {
                    periodSites.add(r.getSiteNo());
                }
            }
        }
        Map<Integer, TPostReportRow> incomingByRowNo = new HashMap<Integer, TPostReportRow>();
        Set<String> seen = new HashSet<String>();
        int seq = 0;
        for (TPostReportRow in : incoming) {
            if (in == null) {
                continue;
            }
            seq++;
            String siteNo = in.getSiteNo() == null ? null : in.getSiteNo().trim();
            if (!isBlank(siteNo) && (seen.contains(siteNo) || periodSites.contains(siteNo))) {
                continue;
            }
            if (!isBlank(siteNo)) {
                seen.add(siteNo);
            }
            incomingByRowNo.put(Integer.valueOf(seq), in);
        }

        int diffs = 0;
        for (Map.Entry<Integer, TPostReportRow> e : incomingByRowNo.entrySet()) {
            TPostReportRow l = landedByRowNo.get(e.getKey());
            if (l == null || !sameReportContent(l, e.getValue())) {
                diffs++;
            }
        }
        for (Integer rowNo : landedByRowNo.keySet()) {
            if (!incomingByRowNo.containsKey(rowNo)) {
                diffs++;
            }
        }
        TPostReportReceipt view = cloneReceipt(stored);
        view.setOutcome(Integer.valueOf(OUTCOME_REVIEW));
        view.setMessage(diffs == 0
                ? "复核一致：落地情形照旧，未改写一行、未添新行；业务量要改走差异那一路"
                : "复核发现" + diffs + "处对不上：只作复核，未改写一行、未添新行；业务量要改走差异那一路");
        return view;
    }

    private boolean sameReportContent(TPostReportRow a, TPostReportRow b) {
        return eq(a.getSiteNo(), b.getSiteNo())
                && nullSafeEq(a.getQty(), b.getQty())
                && eq(a.getFreq(), b.getFreq())
                && nullSafeEq(a.getPrevDiff(), b.getPrevDiff())
                && nullSafeEq(a.getWriteOff(), b.getWriteOff());
    }

    /** 落账后从库里逐行重算回执：收下 + 挂起 必须等于原期行数 */
    private TPostReportReceipt countReceipt(String batchNo, String period, int merged) {
        List<TPostReportRow> landed = listRows(batchNo);
        int accepted = 0;
        int suspended = 0;
        BigDecimal acceptedQty = BigDecimal.ZERO;
        for (TPostReportRow r : landed) {
            if (r.getStatus() == null) {
                continue;
            }
            if (r.getStatus().intValue() == STATUS_ACCEPTED) {
                accepted++;
                if (r.getQty() != null) {
                    acceptedQty = acceptedQty.add(r.getQty());
                }
            } else if (r.getStatus().intValue() == STATUS_SUSPENDED) {
                suspended++;
            }
        }
        int total = accepted + suspended;
        if (total != landed.size()) {
            // 两个出口只许同一回计算：口径接不平直接抛错整笔回滚，不许页面各数各的
            throw new IllegalStateException("回执口径接不平：库内" + landed.size() + "行，收" + accepted + "挂" + suspended);
        }
        String msg = total > 0 ? "核讫" : "空期";
        return receipt(batchNo, period, OUTCOME_ACCEPTED, msg, total, accepted, suspended, merged, acceptedQty);
    }

    @Override
    public TPostReportReceipt getReceipt(String batchNo) {
        if (isBlank(batchNo)) {
            return null;
        }
        return this.postReportReceiptMapper.selectOne(new QueryWrapper<TPostReportReceipt>()
                .eq("batch_no", batchNo.trim()).eq("del_flag", 0));
    }

    @Override
    public List<TPostReportRow> listRows(String batchNo) {
        return this.postReportRowMapper.selectList(new QueryWrapper<TPostReportRow>()
                .eq("batch_no", batchNo).eq("del_flag", 0).orderByAsc("row_no"));
    }

    @Override
    public List<TPostReportRow> listErrors(String batchNo) {
        return this.postReportRowMapper.selectList(new QueryWrapper<TPostReportRow>()
                .eq("batch_no", batchNo).eq("status", STATUS_SUSPENDED).eq("del_flag", 0)
                .orderByAsc("row_no"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportReconcile reconcile(String period) {
        QueryWrapper<TPostReportRow> rw = new QueryWrapper<TPostReportRow>().eq("del_flag", 0);
        QueryWrapper<TPostReportReceipt> pw = new QueryWrapper<TPostReportReceipt>().eq("del_flag", 0);
        if (!isBlank(period)) {
            rw.eq("period", period.trim());
            pw.eq("period", period.trim());
        }
        List<TPostReportRow> allRows = this.postReportRowMapper.selectList(rw);
        List<TPostReportReceipt> allReceipts = this.postReportReceiptMapper.selectList(pw);

        // 逐行口径，按批次归堆
        Map<String, List<TPostReportRow>> rowsByBatch = new LinkedHashMap<String, List<TPostReportRow>>();
        int rowTotal = 0;
        int rowAccepted = 0;
        int rowSuspended = 0;
        BigDecimal rowQty = BigDecimal.ZERO;
        for (TPostReportRow r : allRows) {
            List<TPostReportRow> bucket = rowsByBatch.get(r.getBatchNo());
            if (bucket == null) {
                bucket = new ArrayList<TPostReportRow>();
                rowsByBatch.put(r.getBatchNo(), bucket);
            }
            bucket.add(r);
            rowTotal++;
            if (r.getStatus() != null && r.getStatus().intValue() == STATUS_ACCEPTED) {
                rowAccepted++;
                if (r.getQty() != null) {
                    rowQty = rowQty.add(r.getQty());
                }
            } else if (r.getStatus() != null && r.getStatus().intValue() == STATUS_SUSPENDED) {
                rowSuspended++;
            }
        }

        ReportReconcile result = new ReportReconcile();
        result.setPeriod(isBlank(period) ? null : period.trim());
        result.setRowQty(rowQty);

        int receiptTotal = 0;
        int receiptAccepted = 0;
        int receiptSuspended = 0;
        BigDecimal receiptQty = BigDecimal.ZERO;
        boolean balanced = true;
        for (TPostReportReceipt rc : allReceipts) {
            // 空期、退回、复核回执不参与本期行数口径
            if (rc.getOutcome() == null || rc.getOutcome().intValue() != OUTCOME_ACCEPTED) {
                continue;
            }
            receiptTotal += nz(rc.getTotalRows());
            receiptAccepted += nz(rc.getAcceptedRows());
            receiptSuspended += nz(rc.getSuspendedRows());
            if (rc.getAcceptedQty() != null) {
                receiptQty = receiptQty.add(rc.getAcceptedQty());
            }
            List<TPostReportRow> bucket = rowsByBatch.get(rc.getBatchNo());
            int a = 0;
            int s = 0;
            BigDecimal q = BigDecimal.ZERO;
            if (bucket != null) {
                for (TPostReportRow r : bucket) {
                    if (r.getStatus() != null && r.getStatus().intValue() == STATUS_ACCEPTED) {
                        a++;
                        if (r.getQty() != null) {
                            q = q.add(r.getQty());
                        }
                    } else if (r.getStatus() != null && r.getStatus().intValue() == STATUS_SUSPENDED) {
                        s++;
                    }
                }
            }
            int t = a + s;
            // 差在哪几批哪几行当场点出来，不许拿一个比例摊到全期
            if (t != nz(rc.getTotalRows()) || a != nz(rc.getAcceptedRows())
                    || s != nz(rc.getSuspendedRows()) || q.compareTo(rc.getAcceptedQty() == null ? BigDecimal.ZERO : rc.getAcceptedQty()) != 0) {
                balanced = false;
                result.getMismatchedReceipts().add(rc);
                if (bucket != null) {
                    result.getMismatchedRows().addAll(bucket);
                }
            }
        }
        // 有逐行落账却没有回执（老三案：收后被动过）也点出来
        for (Map.Entry<String, List<TPostReportRow>> e : rowsByBatch.entrySet()) {
            boolean hasAcceptedReceipt = false;
            for (TPostReportReceipt rc : allReceipts) {
                if (e.getKey() != null && e.getKey().equals(rc.getBatchNo())
                        && rc.getOutcome() != null && rc.getOutcome().intValue() == OUTCOME_ACCEPTED) {
                    hasAcceptedReceipt = true;
                }
            }
            if (!hasAcceptedReceipt) {
                balanced = false;
                result.getMismatchedRows().addAll(e.getValue());
            }
        }

        if (rowQty.compareTo(receiptQty) != 0
                || rowTotal != receiptTotal || rowAccepted != receiptAccepted || rowSuspended != receiptSuspended) {
            balanced = false;
        }

        result.setBalanced(balanced);
        result.setReceiptQty(receiptQty);
        result.setRowTotal(rowTotal);
        result.setRowAccepted(rowAccepted);
        result.setRowSuspended(rowSuspended);
        result.setReceiptTotal(receiptTotal);
        result.setReceiptAccepted(receiptAccepted);
        result.setReceiptSuspended(receiptSuspended);
        result.setDiffQty(rowQty.subtract(receiptQty));
        return result;
    }

    private void persistReceipt(TPostReportReceipt receipt) {
        receipt.setDelFlag(0);
        this.postReportReceiptMapper.insert(receipt);
    }

    private TPostReportReceipt cloneReceipt(TPostReportReceipt s) {
        TPostReportReceipt r = receipt(s.getBatchNo(), s.getPeriod(), s.getOutcome(), s.getMessage(),
                nz(s.getTotalRows()), nz(s.getAcceptedRows()), nz(s.getSuspendedRows()),
                nz(s.getMergedRows()), s.getAcceptedQty());
        r.setId(s.getId());
        return r;
    }

    private TPostReportReceipt receipt(String batchNo, String period, int outcome, String message,
                                       int total, int accepted, int suspended, int merged, BigDecimal acceptedQty) {
        TPostReportReceipt r = new TPostReportReceipt();
        r.setBatchNo(batchNo);
        r.setPeriod(period);
        r.setOutcome(Integer.valueOf(outcome));
        r.setMessage(message);
        r.setTotalRows(Integer.valueOf(total));
        r.setAcceptedRows(Integer.valueOf(accepted));
        r.setSuspendedRows(Integer.valueOf(suspended));
        r.setMergedRows(Integer.valueOf(merged));
        r.setAcceptedQty(acceptedQty);
        return r;
    }

    private String reasonOf(String itemCode) {
        if (ITEM_OUTLET_NOT_OWNED.equals(itemCode)) {
            return "网点不在本企业名下，挂起本行";
        }
        if (ITEM_QTY_EMPTY.equals(itemCode)) {
            return "业务量空着没报，挂起本行";
        }
        if (ITEM_QTY_NONPOSITIVE.equals(itemCode)) {
            return "业务量报成零或负数，挂起本行";
        }
        if (ITEM_PREV_DIFF_OPEN.equals(itemCode)) {
            return "上期差异还没核完又来报本期，挂起本行";
        }
        if (ITEM_WRITEOFF_EXCEED.equals(itemCode)) {
            return "本期核销数大过上期挂下的差异，多出的不许塞本行也不许顶邻行";
        }
        return itemCode;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v.intValue();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean eq(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean nullSafeEq(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return a.compareTo(b) == 0;
    }
}
