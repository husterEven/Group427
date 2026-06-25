package com.forum.controller;

import com.forum.common.GlobalExceptionHandler;
import com.forum.common.PageResult;
import com.forum.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController 通知接口 单元测试")
class NotificationControllerTest {

    @Mock private NotificationService notificationService;
    @InjectMocks private NotificationController notificationController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/notifications")
    void getNotifications() throws Exception {
        when(notificationService.getNotifications(1, 20))
                .thenReturn(PageResult.of(Collections.emptyList(), 0, 1, 20));
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/unread-count")
    void getUnreadCount() throws Exception {
        when(notificationService.getUnreadCount()).thenReturn(5);
        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(5));
    }

    @Test
    @DisplayName("PUT /api/v1/notifications/{id}/read")
    void markRead() throws Exception {
        doNothing().when(notificationService).markRead(1L);
        mockMvc.perform(put("/api/v1/notifications/1/read")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/notifications/read-all")
    void markAllRead() throws Exception {
        doNothing().when(notificationService).markAllRead();
        mockMvc.perform(put("/api/v1/notifications/read-all")).andExpect(status().isOk());
    }
}
