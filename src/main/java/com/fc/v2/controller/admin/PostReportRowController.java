package com.fc.v2.controller.admin;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TPostReportRow;
import com.fc.v2.model.custom.ReportBook;
import com.fc.v2.model.custom.ReportReconcile;
import com.fc.v2.service.ITPostReportRowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 月度业务报表核收 Controller。
 * 从进门到落账只走整册处理方法一条道：界面没有单条补录/改写/删除的把手，
 * 回执三数直接取自服务层同一回计算，页面自己数出来的不作数。
 *
 * @author fuce
 * @date 2026-09-29
 */
@Api(value = "月度业务报表核收")
@Controller
@RequestMapping("/PostReportRowController")
public class PostReportRowController extends BaseController {

    private final String prefix = "admin/postReportRow";

    @Autowired
    private ITPostReportRowService postReportRowService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("post:postReportRow:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "月度业务报表整册报送", action = "submitBook")
    @ApiOperation(value = "整册报送（唯一落账入口）", notes = "整册报送")
    @PostMapping("/submitBook")
    @RequiresPermissions("post:postReportRow:submit")
    @ResponseBody
    public AjaxResult submitBook(@RequestBody ReportBook book) {
        return AjaxResult.successData(200, postReportRowService.submitBatch(book));
    }

    @ApiOperation(value = "取批次回执", notes = "共几行/收几行/挂几行以服务层回执为准")
    @GetMapping("/receipt")
    @RequiresPermissions("post:postReportRow:view")
    @ResponseBody
    public AjaxResult receipt(@RequestParam("batchNo") String batchNo) {
        return AjaxResult.successData(200, postReportRowService.getReceipt(batchNo));
    }

    @ApiOperation(value = "批次逐行落账", notes = "按进门行次升序")
    @GetMapping("/rows")
    @RequiresPermissions("post:postReportRow:view")
    @ResponseBody
    public ResultTable rows(@RequestParam("batchNo") String batchNo) {
        List<TPostReportRow> rows = postReportRowService.listRows(batchNo);
        // 总数直接用服务层给的行数，不由表格另数
        return ResultTable.pageTable(rows.size(), rows);
    }

    @ApiOperation(value = "批次挂起行明细", notes = "只挂不合格的那几行")
    @GetMapping("/suspended")
    @RequiresPermissions("post:postReportRow:view")
    @ResponseBody
    public ResultTable suspended(@RequestParam("batchNo") String batchNo) {
        List<TPostReportRow> rows = postReportRowService.listErrors(batchNo);
        return ResultTable.pageTable(rows.size(), rows);
    }

    @Log(title = "月度业务报表全辖对账", action = "reconcile")
    @ApiOperation(value = "全辖对账", notes = "回执口径同库里逐行比，差在几行点几行")
    @GetMapping("/reconcile")
    @RequiresPermissions("post:postReportRow:reconcile")
    @ResponseBody
    public AjaxResult reconcile(@RequestParam(value = "period", required = false) String period) {
        ReportReconcile result = postReportRowService.reconcile(period);
        return AjaxResult.successData(200, result);
    }
}
