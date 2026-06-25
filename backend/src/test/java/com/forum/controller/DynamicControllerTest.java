package com.forum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.common.GlobalExceptionHandler;
import com.forum.common.PageResult;
import com.forum.dto.DynamicCreateRequest;
import com.forum.entity.RealtimeDynamic;
import com.forum.service.DynamicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DynamicController 动态接口 单元测试")
class DynamicControllerTest {

    @Mock private DynamicService dynamicService;
    @InjectMocks private DynamicController dynamicController;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dynamicController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/dynamics")
    class GetFeed {
        @Test
        @DisplayName("默认 latest 过滤")
        void defaultFilter() throws Exception {
            when(dynamicService.getFeed(1, 20, "latest"))
                    .thenReturn(PageResult.of(Collections.emptyList(), 0, 1, 20));
            mockMvc.perform(get("/api/v1/dynamics"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("following 过滤")
        void followingFilter() throws Exception {
            when(dynamicService.getFeed(1, 20, "following"))
                    .thenReturn(PageResult.of(Collections.emptyList(), 0, 1, 20));
            mockMvc.perform(get("/api/v1/dynamics").param("filter", "following"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("hot 过滤")
        void hotFilter() throws Exception {
            when(dynamicService.getFeed(1, 10, "hot"))
                    .thenReturn(PageResult.of(Collections.emptyList(), 0, 1, 10));
            mockMvc.perform(get("/api/v1/dynamics").param("filter", "hot").param("pageSize", "10"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/dynamics")
    class CreateDynamic {
        @Test
        @DisplayName("发布动态应返回 201")
        void createDynamic_shouldReturn201() throws Exception {
            RealtimeDynamic d = new RealtimeDynamic();
            d.setDynamicId(1L);
            d.setContent("hello");
            when(dynamicService.createDynamic(any(DynamicCreateRequest.class))).thenReturn(d);
            mockMvc.perform(post("/api/v1/dynamics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"hello\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("发布成功"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/dynamics/{dynamicId}")
    class DeleteDynamic {
        @Test
        @DisplayName("删除动态")
        void deleteDynamic() throws Exception {
            doNothing().when(dynamicService).deleteDynamic(1L);
            mockMvc.perform(delete("/api/v1/dynamics/1"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("已删除"));
        }

        @Test
        @DisplayName("删除不存在动态")
        void deleteNonExistent() throws Exception {
            doThrow(new RuntimeException("动态不存在")).when(dynamicService).deleteDynamic(999L);
            mockMvc.perform(delete("/api/v1/dynamics/999"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("动态不存在"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/dynamics/user/{userId}")
    class GetByUser {
        @Test
        @DisplayName("查看用户动态")
        void getByUser() throws Exception {
            when(dynamicService.getByUser(1L)).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/v1/dynamics/user/1")).andExpect(status().isOk());
        }
    }
}
