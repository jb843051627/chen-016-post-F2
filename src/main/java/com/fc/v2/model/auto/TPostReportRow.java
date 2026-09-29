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

    /** 报送期（如2026-09），同一期可分多个批次号报送 */
    @TableField("period")
    @ApiModelProperty(value = "报送期")
    private String period;

    /** 月度业务报表报送批次号 */
    @TableField("batch_no")
    @ApiModelProperty(value = "月度业务报表报送批次号")
    private String batchNo;

    /** 进门行次（按进门先后钉死，合并重复行认前头那一个） */
    @TableField("row_no")
    @ApiModelProperty(value = "进门行次")
    private Integer rowNo;

    /** 本行网点行址代号（须在报送企业名下的网点备案卡） */
    @TableField("site_no")
    @ApiModelProperty(value = "本行网点行址代号")
    private String siteNo;

    /** 挂起前置项代号（收下为空） */
    @TableField("item_code")
    @ApiModelProperty(value = "挂起前置项代号")
    private String itemCode;

    /** 本行报送业务量(万件) */
    @TableField("qty")
    @ApiModelProperty(value = "本行报送业务量(万件)")
    private BigDecimal qty;

    /** 投递频次 1每周五日 2每日 */
    @TableField("freq")
    @ApiModelProperty(value = "投递频次 1每周五日 2每日")
    private Integer freq;

    /** 上一期挂下来的那笔差异(万件) */
    @TableField("prev_diff")
    @ApiModelProperty(value = "上一期挂下来的那笔差异(万件)")
    private BigDecimal prevDiff;

    /** 本期核销掉的上期差异(万件)，不得大过prevDiff */
    @TableField("write_off")
    @ApiModelProperty(value = "本期核销掉的上期差异(万件)")
    private BigDecimal writeOff;

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

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 报送企业底档字号（仅随整册入参传入，不入库；落库在批次回执上） */
    @TableField(exist = false)
    @ApiModelProperty(value = "报送企业底档字号（仅入参）")
    private String companySiteNo;

    public String getCompanySiteNo() {
        return companySiteNo;
    }

    public void setCompanySiteNo(String companySiteNo) {
        this.companySiteNo = companySiteNo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
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

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public Integer getFreq() {
        return freq;
    }

    public void setFreq(Integer freq) {
        this.freq = freq;
    }

    public BigDecimal getPrevDiff() {
        return prevDiff;
    }

    public void setPrevDiff(BigDecimal prevDiff) {
        this.prevDiff = prevDiff;
    }

    public BigDecimal getWriteOff() {
        return writeOff;
    }

    public void setWriteOff(BigDecimal writeOff) {
        this.writeOff = writeOff;
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
