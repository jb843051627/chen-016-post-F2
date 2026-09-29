package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.List;

import com.fc.v2.model.auto.TPostReportRow;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 月度业务报表整册入参：一期一沓，头里是批次号，往下逐行。
 * 一个月前后两三沓属常事，各沓另起批次号，各行归各行。
 *
 * @author fuce
 * @date 2026-09-29
 */
@ApiModel(value = "ReportBook", description = "月度业务报表整册入参")
public class ReportBook implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 报送批次号（一沓一号，同号第二次递入只当复核） */
    @ApiModelProperty(value = "报送批次号")
    private String batchNo;

    /** 报送期，如 2026-09 */
    @ApiModelProperty(value = "报送期")
    private String period;

    /** 报送企业底档字号；行内网点须挂在它名下 */
    @ApiModelProperty(value = "报送企业底档字号")
    private String companySiteNo;

    /** 整册各行（进门先后即行次） */
    @ApiModelProperty(value = "整册各行")
    private List<TPostReportRow> rows;

    public ReportBook() {
    }

    /** 旧递法（batchNo + 逐行）原样接得住：报送企业取各行随带的非持久字段 companySiteNo */
    public static ReportBook of(String batchNo, List<TPostReportRow> rows) {
        ReportBook book = new ReportBook();
        book.setBatchNo(batchNo);
        book.setRows(rows);
        if (rows != null) {
            for (TPostReportRow r : rows) {
                if (r != null) {
                    if (book.getPeriod() == null && r.getPeriod() != null) {
                        book.setPeriod(r.getPeriod());
                    }
                    if (book.getCompanySiteNo() == null && r.getCompanySiteNo() != null) {
                        book.setCompanySiteNo(r.getCompanySiteNo());
                    }
                }
            }
        }
        return book;
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

    public String getCompanySiteNo() {
        return companySiteNo;
    }

    public void setCompanySiteNo(String companySiteNo) {
        this.companySiteNo = companySiteNo;
    }

    public List<TPostReportRow> getRows() {
        return rows;
    }

    public void setRows(List<TPostReportRow> rows) {
        this.rows = rows;
    }
}
