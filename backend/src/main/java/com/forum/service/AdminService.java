package com.forum.service;

import com.forum.dto.AuditActionRequest;
import com.forum.dto.ReportHandleRequest;
import com.forum.common.PageResult;
import com.forum.entity.AuditQueue;
import com.forum.entity.Report;
import com.forum.entity.UserPunishment;

import java.util.Map;

public interface AdminService {

    PageResult<AuditQueue> getAuditQueue(int page, int pageSize);

    void audit(Long auditItemId, AuditActionRequest req);

    PageResult<Report> getReports(int page, int pageSize);

    void handleReport(Long reportId, ReportHandleRequest req);

    PageResult<UserPunishment> getPunishments(int page, int pageSize);

    void revokePunishment(Long punishmentId);

    Map<String, Object> getDashboard();
}
