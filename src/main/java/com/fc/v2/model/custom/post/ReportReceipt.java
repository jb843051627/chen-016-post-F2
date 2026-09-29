package com.fc.v2.model.custom.post;

import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 整册核收回执。三个数——原期行数、已收行数、挂起行数——由业务服务层对库内
 * 逐行同回一次算齐：挂起 + 已收 = 原期行数。界面不得自己数。
 *
 * 落点结论 outcome：
 * BOOKED 已落账 / REVIEW 复核不改写 / EMPTY 空期 / REJECTED 整期退回。
 *
 * @author fuce
 * @date 2026-09-29
 */
@ApiModel(value = "ReportReceipt", description = "整册核收回执")
public class ReportReceipt {

    /** 已落账（逐行核收完毕，挂起行各带代号） */
    public static final String BOOKED = "BOOKED";

    /** 同批次号再报，只复核，不改写不添行 */
    public static final String REVIEW = "REVIEW";

    /** 空期：一行也没有，回执只写空期，不许报核讫 */
    public static final String EMPTY = "EMPTY";

    /** 越线整期退回，一行不留 */
    public static final String REJECTED = "REJECTED";

    @ApiModelProperty(value = "报送期号")
    private String periodNo;

    @ApiModelProperty(value = "报送批次号")
    private String batchNo;

    @ApiModelProperty(value = "落点结论 BOOKED/REVIEW/EMPTY/REJECTED")
    private String outcome;

    @ApiModelProperty(value = "结论文字")
    private String outcomeText;

    @ApiModelProperty(value = "原期行数（库内逐行）")
    private int totalRows;

    @ApiModelProperty(value = "已收行数（库内逐行）")
    private int acceptedRows;

    @ApiModelProperty(value = "挂起行数（库内逐行）")
    private int suspendedRows;

    @ApiModelProperty(value = "挂起明细：直接报本期行序号")
    private List<SuspendedLine> suspended = new ArrayList<>();

    @ApiModelProperty(value = "复核逐行不一致明细（不改写）")
    private List<ReviewDiff> reviewDiffs = new ArrayList<>();

    @ApiModel(value = "ReceiptSuspendedLine")
    public static class SuspendedLine {
        /** 本期行序号 */
        private Integer rowNo;
        private String outletNo;
        private String itemCode;
        private String reason;

        public SuspendedLine() {
        }

        public SuspendedLine(Integer rowNo, String outletNo, String itemCode, String reason) {
            this.rowNo = rowNo;
            this.outletNo = outletNo;
            this.itemCode = itemCode;
            this.reason = reason;
        }

        public Integer getRowNo() {
            return rowNo;
        }

        public void setRowNo(Integer rowNo) {
            this.rowNo = rowNo;
        }

        public String getOutletNo() {
            return outletNo;
        }

        public void setOutletNo(String outletNo) {
            this.outletNo = outletNo;
        }

        public String getItemCode() {
            return itemCode;
        }

        public void setItemCode(String itemCode) {
            this.itemCode = itemCode;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    @ApiModel(value = "ReceiptReviewDiff")
    public static class ReviewDiff {
        private Integer rowNo;
        private String outletNo;
        private String field;
        private String stored;
        private String submitted;

        public ReviewDiff() {
        }

        public ReviewDiff(Integer rowNo, String outletNo, String field, String stored, String submitted) {
            this.rowNo = rowNo;
            this.outletNo = outletNo;
            this.field = field;
            this.stored = stored;
            this.submitted = submitted;
        }

        public Integer getRowNo() {
            return rowNo;
        }

        public void setRowNo(Integer rowNo) {
            this.rowNo = rowNo;
        }

        public String getOutletNo() {
            return outletNo;
        }

        public void setOutletNo(String outletNo) {
            this.outletNo = outletNo;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getStored() {
            return stored;
        }

        public void setStored(String stored) {
            this.stored = stored;
        }

        public String getSubmitted() {
            return submitted;
        }

        public void setSubmitted(String submitted) {
            this.submitted = submitted;
        }
    }

    public String getPeriodNo() {
        return periodNo;
    }

    public void setPeriodNo(String periodNo) {
        this.periodNo = periodNo;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getOutcomeText() {
        return outcomeText;
    }

    public void setOutcomeText(String outcomeText) {
        this.outcomeText = outcomeText;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getAcceptedRows() {
        return acceptedRows;
    }

    public void setAcceptedRows(int acceptedRows) {
        this.acceptedRows = acceptedRows;
    }

    public int getSuspendedRows() {
        return suspendedRows;
    }

    public void setSuspendedRows(int suspendedRows) {
        this.suspendedRows = suspendedRows;
    }

    public List<SuspendedLine> getSuspended() {
        return suspended;
    }

    public void setSuspended(List<SuspendedLine> suspended) {
        this.suspended = suspended;
    }

    public List<ReviewDiff> getReviewDiffs() {
        return reviewDiffs;
    }

    public void setReviewDiffs(List<ReviewDiff> reviewDiffs) {
        this.reviewDiffs = reviewDiffs;
    }
}
