package com.fc.v2.controller.admin;

import java.util.List;

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

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TPostReportRow;
import com.fc.v2.model.custom.post.ReportBook;
import com.fc.v2.model.custom.post.ReportReceipt;
import com.fc.v2.model.custom.post.ReportReconcile;
import com.fc.v2.model.custom.post.ScreenCount;
import com.fc.v2.service.ITPostReportRowService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 月度业务报表整册核收 Controller。
 * 只开整册报送、期内逐行查询、回执、全辖对账四个口；不设单条增删改把手——
 * 落账只走业务服务层 acceptBook 那一个整册方法。
 *
 * @author fuce
 * @date 2026-09-29
 */
@Api(value = "月度业务报表整册核收")
@Controller
@RequestMapping("/PostReportRowController")
public class PostReportRowController extends BaseController {

    private final String prefix = "admin/postReportRow";

    @Autowired
    private ITPostReportRowService postReportRowService;

    @ApiOperation(value = "分页跳转", notes = "整册报送页跳转")
    @GetMapping("/view")
    @RequiresPermissions("post:postReportRow:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "月度业务报表整册报送", action = "submit")
    @ApiOperation(value = "整册报送", notes = "一期一沓走 acceptBook 唯一通道")
    @PostMapping("/submit")
    @RequiresPermissions("post:postReportRow:submit")
    @ResponseBody
    public AjaxResult submit(@RequestBody ReportBook book) {
        ReportReceipt receipt = postReportRowService.acceptBook(book);
        AjaxResult result = AjaxResult.successData(200, receipt);
        result.put("msg", receipt.getOutcomeText());
        return result;
    }

    @Log(title = "月度业务报表期内逐行查询", action = "list")
    @ApiOperation(value = "期内逐行", notes = "按行序号升序，挂起笔直接报本期行序号")
    @PostMapping("/list")
    @RequiresPermissions("post:postReportRow:list")
    @ResponseBody
    public List<TPostReportRow> list(@RequestParam String periodNo) {
        return postReportRowService.listPeriodRows(periodNo);
    }

    @Log(title = "月度业务报表回执", action = "receipt")
    @ApiOperation(value = "期内回执", notes = "三数由服务层对库一次算齐，页面不自己数")
    @PostMapping("/receipt")
    @RequiresPermissions("post:postReportRow:list")
    @ResponseBody
    public ReportReceipt receipt(@RequestParam String periodNo) {
        return postReportRowService.receipt(periodNo);
    }

    @Log(title = "全辖对账", action = "reconcile")
    @ApiOperation(value = "全辖对账", notes = "屏上总数 vs 已收逐行加总，不平逐企业点到行")
    @PostMapping("/reconcile")
    @RequiresPermissions("post:postReportRow:reconcile")
    @ResponseBody
    public ReportReconcile reconcile(@RequestParam String periodNo,
                                     @RequestBody(required = false) List<ScreenCount> screen) {
        return postReportRowService.reconcile(periodNo, screen);
    }
}
