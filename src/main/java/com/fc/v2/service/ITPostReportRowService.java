package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TPostReportRow;
import com.fc.v2.model.custom.post.ReportBook;
import com.fc.v2.model.custom.post.ReportReceipt;
import com.fc.v2.model.custom.post.ReportReconcile;
import com.fc.v2.model.custom.post.ScreenCount;

/**
 * 月度业务报表核收行 Service。
 * 从进门到落账只走一条道：{@link #acceptBook(ReportBook)} 这个整册处理方法——
 * 某一行收下、挂起，还是随整期退回，都在这一个方法里定夺；共几行、收了几行、
 * 挂了几行由它一次报齐，不留单条增删改的把手。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITPostReportRowService {

    /** 按主键回查单行 */
    TPostReportRow selectTPostReportRowById(Long id);

    /**
     * 整册核收（唯一落账通道，新递法）。
     * 一个批次号再递只当复核：逐行比对，不改写任何一行落地情形，也不添新行。
     */
    ReportReceipt acceptBook(ReportBook book);

    /**
     * 整册核收（旧递法，入参照旧接得住）：批次号 + 逐行，内部仍走同一个整册方法。
     * 兼容旧约：返回收下的行数；空期返 0；越线整期退回返 -1；重复批次不重复入库。
     */
    int submitBatch(String batchNo, List<TPostReportRow> rows);

    /** 期内逐行（按本期行序号升序，挂起笔直接带着行序号） */
    List<TPostReportRow> listPeriodRows(String periodNo);

    /** 该批次的挂起行明细（按行号升序，旧约保留） */
    List<TPostReportRow> listErrors(String batchNo);

    /** 回执：三个数对库内逐行同回一次算齐，页面不得自己数 */
    ReportReceipt receipt(String periodNo);

    /** 全辖对账：屏上总数与库内已收逐行加总比，不平逐企业点到行，不按比例摊 */
    ReportReconcile reconcile(String periodNo, List<ScreenCount> screen);
}
