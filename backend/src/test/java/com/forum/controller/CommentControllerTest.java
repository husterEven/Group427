package com.forum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.common.GlobalExceptionHandler;
import com.forum.common.PageResult;
import com.forum.dto.CommentCreateRequest;
import com.forum.entity.Comment;
import com.forum.service.CommentService;
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
@DisplayName("CommentController 评论接口 单元测试")
class CommentControllerTest {

    @Mock private CommentService commentService;
    @InjectMocks private CommentController commentController;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/posts/{postId}/comments")
    class GetComments {

        @Test
        @DisplayName("默认参数应返回分页结果")
        void defaultParams_shouldReturnPageResult() throws Exception {
            when(commentService.getComments(1L, 1, 20, "latest"))
                    .thenReturn(PageResult.of(Collections.emptyList(), 0, 1, 20));

            mockMvc.perform(get("/api/v1/posts/1/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("指定 sort=likeCount")
        void withLikeCountSort() throws Exception {
            when(commentService.getComments(1L, 1, 10, "likeCount"))
                    .thenReturn(PageResult.of(Collections.emptyList(), 0, 1, 10));

            mockMvc.perform(get("/api/v1/posts/1/comments")
                            .param("sort", "likeCount")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/posts/{postId}/comments")
    class CreateComment {

        @Test
        @DisplayName("创建评论应返回 201")
        void createComment_shouldReturn201() throws Exception {
            Comment comment = new Comment();
            comment.setCommentId(1L);
            comment.setContent("nice post");
            comment.setPostId(1L);

            when(commentService.createComment(eq(1L), any(CommentCreateRequest.class))).thenReturn(comment);

            mockMvc.perform(post("/api/v1/posts/1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"nice post\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("评论成功"))
                    .andExpect(jsonPath("$.data.content").value("nice post"));
        }

        @Test
        @DisplayName("回复评论（楼中楼）")
        void createReplyComment_shouldReturn201() throws Exception {
            Comment comment = new Comment();
            comment.setCommentId(2L);
            comment.setContent("reply");
            comment.setParentCommentId(10L);

            when(commentService.createComment(eq(1L), any(CommentCreateRequest.class))).thenReturn(comment);

            mockMvc.perform(post("/api/v1/posts/1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"reply\",\"parentCommentId\":10}"))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("评论内容为空应返回 400")
        void blankContent_shouldReturn400() throws Exception {
            mockMvc.perform(post("/api/v1/posts/1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/posts/{postId}/comments/{commentId}")
    class DeleteComment {

        @Test
        @DisplayName("删除评论应返回成功")
        void deleteComment_shouldReturn200() throws Exception {
            doNothing().when(commentService).deleteComment(5L);

            mockMvc.perform(delete("/api/v1/posts/1/comments/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("已删除"));
        }

        @Test
        @DisplayName("删除不存在评论应返回错误")
        void deleteNonExistent_shouldReturnError() throws Exception {
            doThrow(new RuntimeException("评论不存在")).when(commentService).deleteComment(999L);

            mockMvc.perform(delete("/api/v1/posts/1/comments/999"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("评论不存在"));
        }
    }
}
