package com.fc.v2.model.custom.post;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 全辖对账结果：屏上总数与库内已收逐行加总逐企业比对。
 * 不平就把差落在哪一家、哪几行当场点出来，绝不拿一个比例摊到全期。
 *
 * @author fuce
 * @date 2026-09-29
 */
@ApiModel(value = "ReportReconcile", description = "全辖屏总数与逐行加总对账结果")
public class ReportReconcile {

    @ApiModelProperty(value = "报送期号")
    private String periodNo;

    @ApiModelProperty(value = "全辖屏上总数(万件)")
    private BigDecimal screenTotal;

    @ApiModelProperty(value = "已收逐行加总数(万件)")
    private BigDecimal rowTotal;

    @ApiModelProperty(value = "是否接平")
    private boolean balanced;

    @ApiModelProperty(value = "结论文字")
    private String text;

    @ApiModelProperty(value = "逐企业桶比对")
    private List<BucketDiff> buckets = new ArrayList<>();

    @ApiModelProperty(value = "对不平的行（差落在哪几行）")
    private List<RowMark> diffRows = new ArrayList<>();

    @ApiModel(value = "ReportReconcileBucket")
    public static class BucketDiff {
        private String siteNo;
        private BigDecimal screenQty;
        private BigDecimal rowQty;
        private boolean balanced;
        private List<RowMark> rows = new ArrayList<>();

        public String getSiteNo() {
            return siteNo;
        }

        public void setSiteNo(String siteNo) {
            this.siteNo = siteNo;
        }

        public BigDecimal getScreenQty() {
            return screenQty;
        }

        public void setScreenQty(BigDecimal screenQty) {
            this.screenQty = screenQty;
        }

        public BigDecimal getRowQty() {
            return rowQty;
        }

        public void setRowQty(BigDecimal rowQty) {
            this.rowQty = rowQty;
        }

        public boolean isBalanced() {
            return balanced;
        }

        public void setBalanced(boolean balanced) {
            this.balanced = balanced;
        }

        public List<RowMark> getRows() {
            return rows;
        }

        public void setRows(List<RowMark> rows) {
            this.rows = rows;
        }
    }

    @ApiModel(value = "ReportReconcileRow")
    public static class RowMark {
        private Integer rowNo;
        private String siteNo;
        private String outletNo;
        private BigDecimal qty;

        public RowMark() {
        }

        public RowMark(Integer rowNo, String siteNo, String outletNo, BigDecimal qty) {
            this.rowNo = rowNo;
            this.siteNo = siteNo;
            this.outletNo = outletNo;
            this.qty = qty;
        }

        public Integer getRowNo() {
            return rowNo;
        }

        public void setRowNo(Integer rowNo) {
            this.rowNo = rowNo;
        }

        public String getSiteNo() {
            return siteNo;
        }

        public void setSiteNo(String siteNo) {
            this.siteNo = siteNo;
        }

        public String getOutletNo() {
            return outletNo;
        }

        public void setOutletNo(String outletNo) {
            this.outletNo = outletNo;
        }

        public BigDecimal getQty() {
            return qty;
        }

        public void setQty(BigDecimal qty) {
            this.qty = qty;
        }
    }

    public String getPeriodNo() {
        return periodNo;
    }

    public void setPeriodNo(String periodNo) {
        this.periodNo = periodNo;
    }

    public BigDecimal getScreenTotal() {
        return screenTotal;
    }

    public void setScreenTotal(BigDecimal screenTotal) {
        this.screenTotal = screenTotal;
    }

    public BigDecimal getRowTotal() {
        return rowTotal;
    }

    public void setRowTotal(BigDecimal rowTotal) {
        this.rowTotal = rowTotal;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<BucketDiff> getBuckets() {
        return buckets;
    }

    public void setBuckets(List<BucketDiff> buckets) {
        this.buckets = buckets;
    }

    public List<RowMark> getDiffRows() {
        return diffRows;
    }

    public void setDiffRows(List<RowMark> diffRows) {
        this.diffRows = diffRows;
    }
}
