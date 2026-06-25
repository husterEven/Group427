package com.forum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.common.GlobalExceptionHandler;
import com.forum.common.PageResult;
import com.forum.dto.PostCreateRequest;
import com.forum.dto.PostUpdateRequest;
import com.forum.entity.Post;
import com.forum.service.PostService;
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

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostController 帖子接口 单元测试")
class PostControllerTest {

    @Mock private PostService postService;
    @InjectMocks private PostController postController;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/posts")
    class GetPostList {

        @Test
        @DisplayName("默认参数应返回分页结果")
        void defaultParams_shouldReturnPageResult() throws Exception {
            PageResult<Post> pageResult = PageResult.of(Collections.emptyList(), 0, 1, 20);
            when(postService.getPostList(any())).thenReturn(pageResult);

            mockMvc.perform(get("/api/v1/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(0))
                    .andExpect(jsonPath("$.data.page").value(1));
        }

        @Test
        @DisplayName("指定 sort=hot 和 keyword=投资")
        void withSortAndKeyword() throws Exception {
            when(postService.getPostList(any())).thenReturn(
                    PageResult.of(Collections.emptyList(), 0, 1, 10));

            mockMvc.perform(get("/api/v1/posts")
                            .param("sort", "hot")
                            .param("keyword", "投资")
                            .param("page", "2")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("筛选精华帖")
        void filterEssence() throws Exception {
            when(postService.getPostList(any())).thenReturn(
                    PageResult.of(Collections.emptyList(), 0, 1, 20));

            mockMvc.perform(get("/api/v1/posts").param("isEssence", "true"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/posts")
    class CreatePost {

        @Test
        @DisplayName("创建帖子应返回 201 CREATED")
        void createPost_shouldReturn201() throws Exception {
            Post post = new Post();
            post.setPostId(1L);
            post.setTitle("Test");
            post.setContent("Content");

            when(postService.createPost(any(PostCreateRequest.class))).thenReturn(post);

            PostCreateRequest req = new PostCreateRequest();
            req.setTitle("Test");
            req.setContent("Content");
            req.setContentType(0);
            req.setSectionId(1);

            mockMvc.perform(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("发布成功"))
                    .andExpect(jsonPath("$.data.title").value("Test"));
        }

        @Test
        @DisplayName("标题为空应返回 400")
        void blankTitle_shouldReturn400() throws Exception {
            PostCreateRequest req = new PostCreateRequest();
            req.setTitle("");
            req.setContent("Content");
            req.setContentType(0);
            req.setSectionId(1);

            mockMvc.perform(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/posts/{postId}")
    class GetPostDetail {

        @Test
        @DisplayName("存在的帖子应返回详情")
        void existingPost_shouldReturnDetail() throws Exception {
            Post post = new Post();
            post.setPostId(1L);
            post.setTitle("Detail");
            post.setViewCount(10);

            when(postService.getPostDetail(1L)).thenReturn(post);

            mockMvc.perform(get("/api/v1/posts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Detail"))
                    .andExpect(jsonPath("$.data.viewCount").value(10));
        }

        @Test
        @DisplayName("不存在的帖子应返回 400")
        void nonExistentPost_shouldReturn400() throws Exception {
            when(postService.getPostDetail(999L)).thenThrow(new RuntimeException("帖子不存在"));

            mockMvc.perform(get("/api/v1/posts/999"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("帖子不存在"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/posts/{postId}")
    class UpdatePost {

        @Test
        @DisplayName("更新帖子应返回成功")
        void updatePost_shouldSucceed() throws Exception {
            Post post = new Post();
            post.setPostId(1L);
            post.setTitle("Updated");
            when(postService.updatePost(eq(1L), any(PostUpdateRequest.class))).thenReturn(post);

            PostUpdateRequest req = new PostUpdateRequest();
            req.setTitle("Updated");

            mockMvc.perform(put("/api/v1/posts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("更新成功"));
        }

        @Test
        @DisplayName("非作者编辑应返回错误")
        void nonAuthorCannotUpdate() throws Exception {
            when(postService.updatePost(eq(1L), any(PostUpdateRequest.class)))
                    .thenThrow(new RuntimeException("无权修改他人帖子"));

            PostUpdateRequest req = new PostUpdateRequest();
            req.setTitle("Hacked");

            mockMvc.perform(put("/api/v1/posts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("无权修改他人帖子"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/posts/{postId}")
    class DeletePost {

        @Test
        @DisplayName("删除帖子应返回成功")
        void deletePost_shouldReturn200() throws Exception {
            doNothing().when(postService).deletePost(1L);

            mockMvc.perform(delete("/api/v1/posts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("已删除"));
        }

        @Test
        @DisplayName("删除不存在帖子应返回错误")
        void deleteNonExistent_shouldReturnError() throws Exception {
            doThrow(new RuntimeException("帖子不存在")).when(postService).deletePost(999L);

            mockMvc.perform(delete("/api/v1/posts/999"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("帖子不存在"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/posts/{postId}/like")
    class ToggleLike {

        @Test
        @DisplayName("点赞应返回结果")
        void toggleLike_shouldReturnResult() throws Exception {
            Map<String, Object> result = new HashMap<>();
            result.put("isLiked", true);
            result.put("likeCount", 1);
            when(postService.toggleLike(1L)).thenReturn(result);

            mockMvc.perform(post("/api/v1/posts/1/like"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isLiked").value(true))
                    .andExpect(jsonPath("$.data.likeCount").value(1));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/posts/{postId}/collect")
    class ToggleCollect {

        @Test
        @DisplayName("收藏切换应返回结果")
        void toggleCollect_shouldReturnResult() throws Exception {
            Map<String, Object> result = new HashMap<>();
            result.put("isCollected", true);
            result.put("collectCount", 5);
            when(postService.toggleCollect(1L)).thenReturn(result);

            mockMvc.perform(post("/api/v1/posts/1/collect"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isCollected").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/posts/collections")
    class GetCollections {

        @Test
        @DisplayName("获取收藏列表应返回分页结果")
        void shouldReturnPageResult() throws Exception {
            when(postService.getCollections(1, 20))
                    .thenReturn(PageResult.of(Collections.emptyList(), 0, 1, 20));

            mockMvc.perform(get("/api/v1/posts/collections"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/posts/{postId}/pin")
    class TogglePin {

        @Test
        @DisplayName("置顶操作应返回成功")
        void togglePin_shouldReturn200() throws Exception {
            doNothing().when(postService).togglePin(eq(1L), any());

            mockMvc.perform(put("/api/v1/posts/1/pin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isPinned\": true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("已置顶"));
        }

        @Test
        @DisplayName("取消置顶")
        void unpin_shouldReturn200() throws Exception {
            doNothing().when(postService).togglePin(eq(1L), any());

            mockMvc.perform(put("/api/v1/posts/1/pin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isPinned\": false}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("已取消置顶"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/posts/{postId}/essence")
    class ToggleEssence {

        @Test
        @DisplayName("精华操作应返回成功")
        void toggleEssence_shouldReturn200() throws Exception {
            doNothing().when(postService).toggleEssence(eq(1L), any());

            mockMvc.perform(put("/api/v1/posts/1/essence")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isEssence\": true}"))
                    .andExpect(status().isOk());
        }
    }
}
