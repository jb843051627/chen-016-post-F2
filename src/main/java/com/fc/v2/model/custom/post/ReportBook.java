package com.fc.v2.model.custom.post;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 一沓报表整册进门：报送期号、企业底档字号、本沓批次号、逐行内容。
 * 一个月可先后导入两三沓，每一沓另起批次号，各行归各行。
 *
 * @author fuce
 * @date 2026-09-29
 */
@ApiModel(value = "ReportBook", description = "一沓报表整册入参")
public class ReportBook {

    @ApiModelProperty(value = "报送期号(按企业一期，多沓同属一期)", required = true)
    private String periodNo;

    @ApiModelProperty(value = "报送企业底档字号", required = true)
    private String siteNo;

    @ApiModelProperty(value = "本沓报送批次号(一沓一个)", required = true)
    private String batchNo;

    @ApiModelProperty(value = "逐行内容")
    private List<ReportLine> lines;

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

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public List<ReportLine> getLines() {
        return lines;
    }

    public void setLines(List<ReportLine> lines) {
        this.lines = lines;
    }
}
