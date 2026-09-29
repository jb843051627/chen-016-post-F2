package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TPostReportReceipt;
import com.fc.v2.model.auto.TPostReportRow;
import com.fc.v2.model.custom.ReportBook;
import com.fc.v2.model.custom.ReportReconcile;

/**
 * 月度业务报表核收行 Service接口。
 *
 * 进门到落账只走一条道：{@link #submitBatch(ReportBook)} 整册处理方法——
 * 某行收下、挂起，还是随整期退回，都在这一个方法里定夺；
 * 共几行、收了几行、挂了几行由它同一回报齐（回执），界面自数不作数，
 * 也不留单条增删改的把手。同方法名下保留旧入参重载。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITPostReportRowService {

    /** 按主键回查单行（行次取 rowNo，不靠人对着纸数） */
    TPostReportRow selectTPostReportRowById(Long id);

    /**
     * 旧递法接得住：批次号 + 逐行。语义同 {@link #submitBatch(ReportBook)}，
     * 返回已收行数（挂起不计）；超上限整期退回返回 -1；空期返回 0；
     * 同批次号第二次递入只复核、不改账不添行，返回库内原已收行数。
     */
    int submitBatch(String batchNo, List<TPostReportRow> rows);

    /**
     * 整册处理（唯一落账入口）。
     * 逐行校验：网点不在本企业名下、业务量空着、报成零或负数、上期差异没核完又报本期、
     * 本期核销数大过上期挂下数——各按各的前置项代号只挂那一行，同册别行照常进账。
     * 同一期同网点占两行的并作一行、业务量只记一次、行次认前头。
     * 同批次号二次递入只逐行复核：不改任何一行落地情形，也不给本期添新行。
     * 越行数上限整期退回、一行不留；一行没有只写“空期”，不许报核讫。
     */
    TPostReportReceipt submitBatch(ReportBook book);

    /** 取该批次回执（三数以回执/库内逐行同一回计算为准） */
    TPostReportReceipt getReceipt(String batchNo);

    /** 该批次逐行落账（按进门行次升序） */
    List<TPostReportRow> listRows(String batchNo);

    /** 该批次挂起行明细（按行次升序），旧接口名保留 */
    List<TPostReportRow> listErrors(String batchNo);

    /**
     * 全辖对账：库里逐行加总同回执口径比，接不平就把差在哪几批哪几行当场点出来，
     * 不拿一个比例摊到全期。period 为 null 时核全辖全部期。
     */
    ReportReconcile reconcile(String period);
}
