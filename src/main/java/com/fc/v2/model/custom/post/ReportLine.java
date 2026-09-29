package com.fc.v2.model.custom.post;

import java.math.BigDecimal;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 整册报表里的原始一行：网点行址代号、本期业务量、投递频次、上期差异、本期核销数。
 * 行序号不在入参里——进门那一刻由整册方法按先后钉死。
 *
 * @author fuce
 * @date 2026-09-29
 */
@ApiModel(value = "ReportLine", description = "整册报表原始一行")
public class ReportLine {

    @ApiModelProperty(value = "网点行址代号")
    private String outletNo;

    @ApiModelProperty(value = "本期业务量(万件)")
    private BigDecimal qty;

    @ApiModelProperty(value = "投递频次 5每周五日 7每日")
    private Integer deliveryFreq;

    @ApiModelProperty(value = "上期挂账差异(万件)，空着视作0")
    private BigDecimal prevDiff;

    @ApiModelProperty(value = "本期核销上期差异数(万件)，空着视作0")
    private BigDecimal writeOffAmt;

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

    public Integer getDeliveryFreq() {
        return deliveryFreq;
    }

    public void setDeliveryFreq(Integer deliveryFreq) {
        this.deliveryFreq = deliveryFreq;
    }

    public BigDecimal getPrevDiff() {
        return prevDiff;
    }

    public void setPrevDiff(BigDecimal prevDiff) {
        this.prevDiff = prevDiff;
    }

    public BigDecimal getWriteOffAmt() {
        return writeOffAmt;
    }

    public void setWriteOffAmt(BigDecimal writeOffAmt) {
        this.writeOffAmt = writeOffAmt;
    }
}
