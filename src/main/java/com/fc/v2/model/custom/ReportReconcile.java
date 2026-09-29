package com.fc.v2.model.custom;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fc.v2.model.auto.TPostReportRow;
import com.fc.v2.model.auto.TPostReportReceipt;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 全辖对账结果：整屏总数须与库里逐行加总一个口径，
 * 一旦接不平，把差在哪几行当场点出来，不许拿一个比例摊到全期。
 *
 * @author fuce
 * @date 2026-09-29
 */
@ApiModel(value = "ReportReconcile", description = "全辖报表对账结果")
public class ReportReconcile implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 是否接平 */
    @ApiModelProperty(value = "是否接平")
    private boolean balanced;

    /** 核对范围：报送期；null 表示全辖全部期 */
    @ApiModelProperty(value = "核对报送期")
    private String period;

    /** 库里逐行加出来的已收业务量合计(万件) */
    @ApiModelProperty(value = "逐行已收业务量合计")
    private BigDecimal rowQty;

    /** 各回执快照上已收业务量合计(万件) */
    @ApiModelProperty(value = "回执已收业务量合计")
    private BigDecimal receiptQty;

    /** 逐行口径：总行数 / 已收 / 挂起 */
    @ApiModelProperty(value = "逐行口径行数")
    private int rowTotal;
    @ApiModelProperty(value = "逐行口径已收行数")
    private int rowAccepted;
    @ApiModelProperty(value = "逐行口径挂起行数")
    private int rowSuspended;

    /** 回执口径：总行数 / 已收 / 挂起 */
    @ApiModelProperty(value = "回执口径总行数")
    private int receiptTotal;
    @ApiModelProperty(value = "回执口径已收行数")
    private int receiptAccepted;
    @ApiModelProperty(value = "回执口径挂起行数")
    private int receiptSuspended;

    /** 差量(万件)：逐行 - 回执 */
    @ApiModelProperty(value = "业务量差量(逐行-回执)")
    private BigDecimal diffQty;

    /** 对不上的批次回执 */
    @ApiModelProperty(value = "对不上的批次回执")
    private List<TPostReportReceipt> mismatchedReceipts = new ArrayList<>();

    /** 对不上的具体行（当场点行，不按比例摊） */
    @ApiModelProperty(value = "对不上的具体行")
    private List<TPostReportRow> mismatchedRows = new ArrayList<>();

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public BigDecimal getRowQty() {
        return rowQty;
    }

    public void setRowQty(BigDecimal rowQty) {
        this.rowQty = rowQty;
    }

    public BigDecimal getReceiptQty() {
        return receiptQty;
    }

    public void setReceiptQty(BigDecimal receiptQty) {
        this.receiptQty = receiptQty;
    }

    public int getRowTotal() {
        return rowTotal;
    }

    public void setRowTotal(int rowTotal) {
        this.rowTotal = rowTotal;
    }

    public int getRowAccepted() {
        return rowAccepted;
    }

    public void setRowAccepted(int rowAccepted) {
        this.rowAccepted = rowAccepted;
    }

    public int getRowSuspended() {
        return rowSuspended;
    }

    public void setRowSuspended(int rowSuspended) {
        this.rowSuspended = rowSuspended;
    }

    public int getReceiptTotal() {
        return receiptTotal;
    }

    public void setReceiptTotal(int receiptTotal) {
        this.receiptTotal = receiptTotal;
    }

    public int getReceiptAccepted() {
        return receiptAccepted;
    }

    public void setReceiptAccepted(int receiptAccepted) {
        this.receiptAccepted = receiptAccepted;
    }

    public int getReceiptSuspended() {
        return receiptSuspended;
    }

    public void setReceiptSuspended(int receiptSuspended) {
        this.receiptSuspended = receiptSuspended;
    }

    public BigDecimal getDiffQty() {
        return diffQty;
    }

    public void setDiffQty(BigDecimal diffQty) {
        this.diffQty = diffQty;
    }

    public List<TPostReportReceipt> getMismatchedReceipts() {
        return mismatchedReceipts;
    }

    public void setMismatchedReceipts(List<TPostReportReceipt> mismatchedReceipts) {
        this.mismatchedReceipts = mismatchedReceipts;
    }

    public List<TPostReportRow> getMismatchedRows() {
        return mismatchedRows;
    }

    public void setMismatchedRows(List<TPostReportRow> mismatchedRows) {
        this.mismatchedRows = mismatchedRows;
    }
}
