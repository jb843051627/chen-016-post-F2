package com.fc.v2.model.custom.post;

import java.math.BigDecimal;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 全辖一屏上，某一家企业的已收业务量合计（对账时逐企业递进来，不按比例摊差）。
 *
 * @author fuce
 * @date 2026-09-29
 */
@ApiModel(value = "ScreenCount", description = "全辖屏上某企业已收合计")
public class ScreenCount {

    @ApiModelProperty(value = "企业底档字号")
    private String siteNo;

    @ApiModelProperty(value = "屏上该企业已收业务量合计(万件)")
    private BigDecimal qty;

    public ScreenCount() {
    }

    public ScreenCount(String siteNo, BigDecimal qty) {
        this.siteNo = siteNo;
        this.qty = qty;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }
}
