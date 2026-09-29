package com.fc.v2.model.auto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

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

/**
 * 月度业务报表整册回执对象 t_post_report_receipt
 *
 * @author fuce
 * @date 2026-09-29
 */
@TableName("t_post_report_receipt")
@ApiModel(value = "TPostReportReceipt", description = "月度业务报表整册回执")
public class TPostReportReceipt implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 报送批次号（一沓一号，唯一） */
    @TableField("batch_no")
    @ApiModelProperty(value = "报送批次号")
    private String batchNo;

    /** 报送期 */
    @TableField("period")
    @ApiModelProperty(value = "报送期")
    private String period;

    /** 本期落账总行数（收下+挂起，重复网点已并作一行） */
    @TableField("total_rows")
    @ApiModelProperty(value = "本期落账总行数")
    private Integer totalRows;

    /** 已收行数 */
    @TableField("accepted_rows")
    @ApiModelProperty(value = "已收行数")
    private Integer acceptedRows;

    /** 挂起行数 */
    @TableField("suspended_rows")
    @ApiModelProperty(value = "挂起行数")
    private Integer suspendedRows;

    /** 同网点重复并掉的行数（不计入总数） */
    @TableField("merged_rows")
    @ApiModelProperty(value = "同网点重复并掉的行数")
    private Integer mergedRows;

    /** 已收行业务量合计(万件) */
    @TableField("accepted_qty")
    @ApiModelProperty(value = "已收行业务量合计(万件)")
    private BigDecimal acceptedQty;

    /** 回执情形 1核收 2空期 3整期退回 4复核(不落库) */
    @TableField("outcome")
    @ApiModelProperty(value = "回执情形 1核收 2空期 3整期退回 4复核")
    private Integer outcome;

    /** 回执一句话（空期只写空期，不许报核讫） */
    @TableField("message")
    @ApiModelProperty(value = "回执一句话")
    private String message;

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

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getAcceptedRows() {
        return acceptedRows;
    }

    public void setAcceptedRows(Integer acceptedRows) {
        this.acceptedRows = acceptedRows;
    }

    public Integer getSuspendedRows() {
        return suspendedRows;
    }

    public void setSuspendedRows(Integer suspendedRows) {
        this.suspendedRows = suspendedRows;
    }

    public Integer getMergedRows() {
        return mergedRows;
    }

    public void setMergedRows(Integer mergedRows) {
        this.mergedRows = mergedRows;
    }

    public BigDecimal getAcceptedQty() {
        return acceptedQty;
    }

    public void setAcceptedQty(BigDecimal acceptedQty) {
        this.acceptedQty = acceptedQty;
    }

    public Integer getOutcome() {
        return outcome;
    }

    public void setOutcome(Integer outcome) {
        this.outcome = outcome;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
