package com.forum.controller;

import com.forum.common.Result;
import com.forum.common.SecurityUtil;
import com.forum.dto.ReportCreateRequest;
import com.forum.entity.Report;
import com.forum.mapper.ReportMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final SecurityUtil securityUtil;
    private final ReportMapper reportMapper;

    @PostMapping
    public Result<?> submitReport(@Valid @RequestBody ReportCreateRequest req) {
        Report report = new Report();
        report.setReporterId(securityUtil.getCurrentUserId());
        report.setTargetType(req.getTargetType());
        report.setTargetId(req.getTargetId());
        report.setReason(req.getReason());
        report.setStatus(0);
        report.setCreatedAt(LocalDateTime.now());
        reportMapper.insert(report);
        return Result.ok("举报已提交", null);
    }
}
