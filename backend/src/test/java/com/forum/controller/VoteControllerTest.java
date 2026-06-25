package com.forum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.common.GlobalExceptionHandler;
import com.forum.dto.VoteCreateRequest;
import com.forum.dto.VoteSubmitRequest;
import com.forum.service.VoteService;
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

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoteController 投票接口 单元测试")
class VoteControllerTest {

    @Mock private VoteService voteService;
    @InjectMocks private VoteController voteController;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(voteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/posts/{postId}/vote")
    class CreateVote {
        @Test
        @DisplayName("创建投票应返回成功")
        void createVote_shouldReturn200() throws Exception {
            when(voteService.createVote(eq(1L), any(VoteCreateRequest.class))).thenReturn(null);
            mockMvc.perform(post("/api/v1/posts/1/vote")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"voteTitle\":\"test vote\",\"endTime\":\"2027-12-31T23:59:59\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("创建成功"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/posts/{postId}/vote")
    class GetVoteByPost {
        @Test
        @DisplayName("查看投票详情")
        void getVoteByPost() throws Exception {
            Map<String, Object> result = new HashMap<>();
            result.put("voteId", 1L);
            result.put("voteTitle", "test");
            result.put("isExpired", false);
            when(voteService.getVoteByPost(1L)).thenReturn(result);
            mockMvc.perform(get("/api/v1/posts/1/vote"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.voteId").value(1))
                    .andExpect(jsonPath("$.data.isExpired").value(false));
        }

        @Test
        @DisplayName("不存在的投票应返回错误")
        void nonExistentVote_shouldReturn400() throws Exception {
            when(voteService.getVoteByPost(999L)).thenThrow(new RuntimeException("投票不存在"));
            mockMvc.perform(get("/api/v1/posts/999/vote"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("投票不存在"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/votes/{voteId}/submit")
    class SubmitVote {
        @Test
        @DisplayName("提交投票应返回成功")
        void submitVote_shouldReturn200() throws Exception {
            Map<String, Object> result = new HashMap<>();
            result.put("submitted", true);
            when(voteService.submitVote(eq(1L), any(VoteSubmitRequest.class))).thenReturn(result);
            mockMvc.perform(post("/api/v1/votes/1/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"optionIndex\":0}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("投票成功"));
        }

        @Test
        @DisplayName("重复投票应返回错误")
        void duplicateVote_shouldReturn400() throws Exception {
            when(voteService.submitVote(eq(1L), any(VoteSubmitRequest.class)))
                    .thenThrow(new RuntimeException("您已经投过票了"));
            mockMvc.perform(post("/api/v1/votes/1/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"optionIndex\":0}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("您已经投过票了"));
        }
    }
}
