package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 月度业务报表核收行对象 t_post_report_row
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_post_report_row")
@ApiModel(value = "TPostReportRow", description = "月度业务报表核收行")
public class TPostReportRow implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 报送批次号(一沓一个，同月可先后导入多沓) */
    @TableField("batch_no")
    @ApiModelProperty(value = "报送批次号")
    private String batchNo;

    /** 报送期号(按企业一期，多沓同属一期) */
    @TableField("period_no")
    @ApiModelProperty(value = "报送期号(按企业一期)")
    private String periodNo;

    /** 报送企业底档字号 */
    @TableField("site_no")
    @ApiModelProperty(value = "报送企业底档字号")
    private String siteNo;

    /** 网点行址代号 */
    @TableField("outlet_no")
    @ApiModelProperty(value = "网点行址代号")
    private String outletNo;

    /** 在本期内的行序号（进门先后钉死，合并行认前头那一行） */
    @TableField("row_no")
    @ApiModelProperty(value = "本期行序号")
    private Integer rowNo;

    /** 投递频次 5每周五日 7每日 */
    @TableField("delivery_freq")
    @ApiModelProperty(value = "投递频次 5每周五日 7每日")
    private Integer deliveryFreq;

    /** 本行报送业务量(万件) */
    @TableField("qty")
    @ApiModelProperty(value = "本行报送业务量(万件)")
    private BigDecimal qty;

    /** 上期挂账差异(万件) */
    @TableField("prev_diff")
    @ApiModelProperty(value = "上期挂账差异(万件)")
    private BigDecimal prevDiff;

    /** 本期核销上期差异数(万件) */
    @TableField("write_off_amt")
    @ApiModelProperty(value = "本期核销上期差异数(万件)")
    private BigDecimal writeOffAmt;

    /** 被挑落挂起的前置项代号（见 ReportFrontItem 常量） */
    @TableField("item_code")
    @ApiModelProperty(value = "被挑出的前置项代号")
    private String itemCode;

    /** 行落地情形 0待核 1已收 2挂起 */
    @TableField("status")
    @ApiModelProperty(value = "行落地情形 0待核 1已收 2挂起")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注（挂起缘故一句话落在此处） */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getPeriodNo() {
        return periodNo;
    }

    public void setPeriodNo(String periodNo) {
        this.periodNo = periodNo;
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

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public Integer getDeliveryFreq() {
        return deliveryFreq;
    }

    public void setDeliveryFreq(Integer deliveryFreq) {
        this.deliveryFreq = deliveryFreq;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
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

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
