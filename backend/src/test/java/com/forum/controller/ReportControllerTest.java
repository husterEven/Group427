package com.forum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.common.GlobalExceptionHandler;
import com.forum.common.SecurityUtil;
import com.forum.entity.Report;
import com.forum.mapper.ReportMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportController 举报接口 单元测试")
class ReportControllerTest {

    @Mock private SecurityUtil securityUtil;
    @Mock private ReportMapper reportMapper;
    @InjectMocks private ReportController reportController;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reportController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/reports — 提交举报")
    void submitReport() throws Exception {
        when(securityUtil.getCurrentUserId()).thenReturn(1L);
        when(reportMapper.insert(any(Report.class))).thenReturn(1);

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":0,\"targetId\":100,\"reason\":\"违规内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("举报已提交"));

        verify(reportMapper).insert(any(Report.class));
    }

    @Test
    @DisplayName("未登录应返回错误")
    void unauthenticated() throws Exception {
        when(securityUtil.getCurrentUserId()).thenThrow(new RuntimeException("未登录或Token已过期"));
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":0,\"targetId\":100,\"reason\":\"违规\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("未登录或Token已过期"));
    }
}
