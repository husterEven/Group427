package com.forum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.common.GlobalExceptionHandler;
import com.forum.common.PageResult;
import com.forum.dto.AuditActionRequest;
import com.forum.dto.ReportHandleRequest;
import com.forum.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController 管理接口 单元测试")
class AdminControllerTest {

    @Mock private AdminService adminService;
    @InjectMocks private AdminController adminController;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/audit — 审核队列")
    void getAuditQueue() throws Exception {
        when(adminService.getAuditQueue(1, 20))
                .thenReturn(PageResult.of(Collections.emptyList(), 0, 1, 20));
        mockMvc.perform(get("/api/v1/admin/audit")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/audit/{id} — 审核操作")
    void audit() throws Exception {
        doNothing().when(adminService).audit(eq(1L), any(AuditActionRequest.class));
        mockMvc.perform(put("/api/v1/admin/audit/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auditStatus\":1,\"auditComment\":\"通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports — 举报列表")
    void getReports() throws Exception {
        when(adminService.getReports(1, 20))
                .thenReturn(PageResult.of(Collections.emptyList(), 0, 1, 20));
        mockMvc.perform(get("/api/v1/admin/reports")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/reports/{id} — 处理举报")
    void handleReport() throws Exception {
        doNothing().when(adminService).handleReport(eq(1L), any(ReportHandleRequest.class));
        mockMvc.perform(put("/api/v1/admin/reports/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":1,\"handleResult\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("处理完成"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/punishments — 处罚列表")
    void getPunishments() throws Exception {
        when(adminService.getPunishments(1, 20))
                .thenReturn(PageResult.of(Collections.emptyList(), 0, 1, 20));
        mockMvc.perform(get("/api/v1/admin/punishments")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/punishments/{id}/revoke — 撤销处罚")
    void revokePunishment() throws Exception {
        doNothing().when(adminService).revokePunishment(1L);
        mockMvc.perform(put("/api/v1/admin/punishments/1/revoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("已撤销"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/dashboard — 数据大盘")
    void getDashboard() throws Exception {
        when(adminService.getDashboard()).thenReturn(Map.of("dau", 100, "mau", 1000));
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dau").value(100))
                .andExpect(jsonPath("$.data.mau").value(1000));
    }
}
