package com.fc.v2.model.custom.post;

/**
 * 挂起前置项代号：一行不合格只挂那一行，各按各的代号落挂起档。
 *
 * @author fuce
 * @date 2026-09-29
 */
public final class ReportFrontItem {

    private ReportFrontItem() {
    }

    /** 网点不在本企业名下 */
    public static final String OUTLET_NOT_OWNED = "OUTLET_NOT_OWNED";

    /** 业务量空着没报 */
    public static final String QTY_BLANK = "QTY_BLANK";

    /** 业务量报成零或负数 */
    public static final String QTY_NON_POSITIVE = "QTY_NON_POSITIVE";

    /** 上期差异还没核完又来报这一期 */
    public static final String PREV_DIFF_UNSETTLED = "PREV_DIFF_UNSETTLED";

    /** 本期核销数大过上期挂账数（挂三十不许核四十） */
    public static final String WRITEOFF_EXCEEDS = "WRITEOFF_EXCEEDS";

    /** 代号对应的一句话缘故 */
    public static String reason(String itemCode) {
        if (itemCode == null) {
            return "";
        }
        switch (itemCode) {
            case OUTLET_NOT_OWNED:
                return "网点不在本企业名下";
            case QTY_BLANK:
                return "业务量一栏空着没报";
            case QTY_NON_POSITIVE:
                return "业务量报成零或负数";
            case PREV_DIFF_UNSETTLED:
                return "上期差异尚未核完，暂缓报本期";
            case WRITEOFF_EXCEEDS:
                return "本期核销数大过上期挂账差异数";
            default:
                return itemCode;
        }
    }
}
