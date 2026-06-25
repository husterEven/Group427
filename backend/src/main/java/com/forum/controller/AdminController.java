package com.forum.controller;

import com.forum.common.Result;
import com.forum.dto.AuditActionRequest;
import com.forum.dto.ReportHandleRequest;
import com.forum.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/audit")
    public Result<?> getAuditQueue(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(adminService.getAuditQueue(page, pageSize));
    }

    @PutMapping("/audit/{id}")
    public Result<?> audit(@PathVariable Long id, @Valid @RequestBody AuditActionRequest req) {
        adminService.audit(id, req);
        return Result.ok("操作成功", null);
    }

    @GetMapping("/reports")
    public Result<?> getReports(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(adminService.getReports(page, pageSize));
    }

    @PutMapping("/reports/{id}")
    public Result<?> handleReport(@PathVariable Long id, @Valid @RequestBody ReportHandleRequest req) {
        adminService.handleReport(id, req);
        return Result.ok("处理完成", null);
    }

    @GetMapping("/punishments")
    public Result<?> getPunishments(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(adminService.getPunishments(page, pageSize));
    }

    @PutMapping("/punishments/{id}/revoke")
    public Result<?> revokePunishment(@PathVariable Long id) {
        adminService.revokePunishment(id);
        return Result.ok("已撤销", null);
    }

    @GetMapping("/dashboard")
    public Result<?> getDashboard() {
        return Result.ok(adminService.getDashboard());
    }
}
