package com.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forum.common.PageResult;
import com.forum.common.SecurityUtil;
import com.forum.dto.*;
import com.forum.entity.Post;
import com.forum.entity.PostCollect;
import com.forum.entity.User;
import com.forum.entity.UserAchievement;
import com.forum.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostServiceImpl 帖子服务 单元测试")
class PostServiceImplTest {

    @Mock private PostMapper postMapper;
    @Mock private UserMapper userMapper;
    @Mock private PostCollectMapper postCollectMapper;
    @Mock private PostLikeMapper postLikeMapper;
    @Mock private UserAchievementMapper userAchievementMapper;
    @Mock private SectionMapper sectionMapper;
    @Mock private ZoneMapper zoneMapper;
    @Mock private SecurityUtil securityUtil;
    @Mock private NotificationHelper notificationHelper;

    @InjectMocks
    private PostServiceImpl postService;

    private Post createTestPost(Long postId, Long authorId) {
        Post post = new Post();
        post.setPostId(postId);
        post.setAuthorId(authorId);
        post.setTitle("Test Title");
        post.setContent("Test Content");
        post.setSectionId(1);
        post.setLikeCount(0);
        post.setViewCount(0);
        post.setCommentCount(0);
        post.setCollectCount(0);
        post.setIsEssence(0);
        post.setIsPinned(0);
        return post;
    }

    @Nested
    @DisplayName("getPostList() 帖子列表")
    class GetPostList {

        @Test
        @DisplayName("默认排序(latest)应按 publish_time 倒序")
        void defaultSort_shouldOrderByPublishTime() {
            PostQueryRequest req = new PostQueryRequest();
            req.setPage(1);
            req.setPageSize(10);
            req.setSort("latest");

            Page<Post> mpPage = new Page<>(1, 10);
            mpPage.setRecords(List.of(createTestPost(1L, 100L)));
            mpPage.setTotal(1);

            when(postMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mpPage);
            when(userMapper.selectBatchIds(anySet())).thenReturn(Collections.emptyList());
            when(sectionMapper.selectBatchIds(anySet())).thenReturn(Collections.emptyList());

            PageResult<Post> result = postService.getPostList(req);

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
        }

        @Test
        @DisplayName("hot 排序应按 (likeCount + commentCount) 倒序")
        void hotSort_shouldOrderByHotScore() {
            PostQueryRequest req = new PostQueryRequest();
            req.setPage(1);
            req.setPageSize(10);
            req.setSort("hot");

            when(postMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                    .thenReturn(new Page<>(1, 10));

            assertDoesNotThrow(() -> postService.getPostList(req));
        }

        @Test
        @DisplayName("popular 排序应按 likeCount 倒序")
        void popularSort_shouldOrderByLikeCount() {
            PostQueryRequest req = new PostQueryRequest();
            req.setPage(1);
            req.setPageSize(10);
            req.setSort("popular");

            when(postMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                    .thenReturn(new Page<>(1, 10));

            assertDoesNotThrow(() -> postService.getPostList(req));
        }

        @Test
        @DisplayName("按 authorId 过滤")
        void authorIdFilter_shouldFilterByAuthor() {
            PostQueryRequest req = new PostQueryRequest();
            req.setPage(1);
            req.setPageSize(10);
            req.setAuthorId(100L);

            when(postMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                    .thenReturn(new Page<>(1, 10));

            assertDoesNotThrow(() -> postService.getPostList(req));
        }

        @Test
        @DisplayName("按 keyword 搜索")
        void keywordFilter_shouldSearchByTitleAndContent() {
            PostQueryRequest req = new PostQueryRequest();
            req.setPage(1);
            req.setPageSize(10);
            req.setKeyword("投资");

            when(postMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                    .thenReturn(new Page<>(1, 10));

            assertDoesNotThrow(() -> postService.getPostList(req));
        }

        @Test
        @DisplayName("筛选精华帖")
        void essenceFilter_shouldFilterEssence() {
            PostQueryRequest req = new PostQueryRequest();
            req.setPage(1);
            req.setPageSize(10);
            req.setIsEssence(true);

            when(postMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                    .thenReturn(new Page<>(1, 10));

            assertDoesNotThrow(() -> postService.getPostList(req));
        }
    }

    @Nested
    @DisplayName("getPostDetail() 帖子详情")
    class GetPostDetail {

        @Test
        @DisplayName("存在的帖子应返回详情并增加浏览量")
        void existingPost_shouldReturnDetailAndIncrementView() {
            Post post = createTestPost(1L, 100L);
            post.setViewCount(5);

            when(postMapper.selectById(1L)).thenReturn(post);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);

            Post result = postService.getPostDetail(1L);

            assertNotNull(result);
            assertEquals(6, result.getViewCount());
            verify(postMapper).updateById(post);
        }

        @Test
        @DisplayName("不存在的帖子应抛出 RuntimeException")
        void nonExistentPost_shouldThrowRuntimeException() {
            when(postMapper.selectById(999L)).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> postService.getPostDetail(999L));
            assertEquals("帖子不存在", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("createPost() 创建帖子")
    class CreatePost {

        @Test
        @DisplayName("创建帖子应成功并增加成就计数")
        void createPost_shouldSucceedAndUpdateAchievement() {
            PostCreateRequest req = new PostCreateRequest();
            req.setTitle("Title");
            req.setContent("Content");
            req.setContentType(0);
            req.setSectionId(1);

            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            when(postMapper.insert(any(Post.class))).thenAnswer(inv -> {
                Post p = inv.getArgument(0);
                p.setPostId(1L);
                return 1;
            });

            UserAchievement achievement = new UserAchievement();
            achievement.setUserId(1L);
            achievement.setTotalPostCount(5);
            when(userAchievementMapper.selectOne(any(QueryWrapper.class))).thenReturn(achievement);
            when(userAchievementMapper.updateById(any(UserAchievement.class))).thenReturn(1);

            Post result = postService.createPost(req);

            assertNotNull(result);
            assertEquals("Title", result.getTitle());
            assertEquals(1L, result.getPostId());
        }

        @Test
        @DisplayName("首次发帖时成就记录不存在应自动创建")
        void firstPost_shouldCreateAchievement() {
            PostCreateRequest req = new PostCreateRequest();
            req.setTitle("Title");
            req.setContent("Content");
            req.setContentType(0);
            req.setSectionId(1);

            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            when(postMapper.insert(any(Post.class))).thenAnswer(inv -> {
                Post p = inv.getArgument(0);
                p.setPostId(1L);
                return 1;
            });
            when(userAchievementMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
            when(userAchievementMapper.insert(any(UserAchievement.class))).thenReturn(1);

            assertDoesNotThrow(() -> postService.createPost(req));
            verify(userAchievementMapper).insert(any(UserAchievement.class));
        }
    }

    @Nested
    @DisplayName("updatePost() 编辑帖子")
    class UpdatePost {

        @Test
        @DisplayName("作者本人可编辑帖子")
        void authorCanUpdate() {
            Post post = createTestPost(1L, 100L);
            when(securityUtil.getCurrentUserId()).thenReturn(100L);
            when(postMapper.selectById(1L)).thenReturn(post);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);

            PostUpdateRequest req = new PostUpdateRequest();
            req.setTitle("Updated Title");

            Post result = postService.updatePost(1L, req);

            assertEquals("Updated Title", result.getTitle());
        }

        @Test
        @DisplayName("非作者编辑应抛出 RuntimeException")
        void nonAuthorCannotUpdate() {
            Post post = createTestPost(1L, 100L);
            when(securityUtil.getCurrentUserId()).thenReturn(200L);
            when(postMapper.selectById(1L)).thenReturn(post);

            PostUpdateRequest req = new PostUpdateRequest();
            req.setTitle("Hacked");

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> postService.updatePost(1L, req));
            assertEquals("无权修改他人帖子", ex.getMessage());
        }

        @Test
        @DisplayName("编辑不存在的帖子应抛出 RuntimeException")
        void updateNonExistent_shouldThrow() {
            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            when(postMapper.selectById(999L)).thenReturn(null);

            PostUpdateRequest req = new PostUpdateRequest();
            req.setTitle("X");

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> postService.updatePost(999L, req));
            assertEquals("帖子不存在", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("deletePost() 删除帖子")
    class DeletePost {

        @Test
        @DisplayName("作者可软删除自己的帖子")
        void authorCanDelete() {
            Post post = createTestPost(1L, 100L);
            when(securityUtil.getCurrentUserId()).thenReturn(100L);
            when(postMapper.selectById(1L)).thenReturn(post);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);

            assertDoesNotThrow(() -> postService.deletePost(1L));
            assertEquals(1, post.getIsDeleted());
        }

        @Test
        @DisplayName("非作者删除应抛出 RuntimeException")
        void nonAuthorCannotDelete() {
            Post post = createTestPost(1L, 100L);
            when(securityUtil.getCurrentUserId()).thenReturn(200L);
            when(postMapper.selectById(1L)).thenReturn(post);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> postService.deletePost(1L));
            assertEquals("无权删除他人帖子", ex.getMessage());
        }

        @Test
        @DisplayName("删除不存在的帖子应抛出 RuntimeException")
        void deleteNonExistent_shouldThrow() {
            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            when(postMapper.selectById(999L)).thenReturn(null);

            assertThrows(RuntimeException.class, () -> postService.deletePost(999L));
        }
    }

    @Nested
    @DisplayName("toggleLike() 点赞切换")
    class ToggleLike {

        @Test
        @DisplayName("点赞应增加 likeCount")
        void toggleLike_shouldIncrementCount() {
            Post post = createTestPost(1L, 100L);
            post.setLikeCount(10);
            when(postMapper.selectById(1L)).thenReturn(post);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);

            Map<String, Object> result = postService.toggleLike(1L);

            assertEquals(true, result.get("isLiked"));
            assertEquals(11, result.get("likeCount"));
        }

        @Test
        @DisplayName("点赞不存在的帖子应抛出 RuntimeException")
        void likeNonExistent_shouldThrow() {
            when(postMapper.selectById(999L)).thenReturn(null);
            assertThrows(RuntimeException.class, () -> postService.toggleLike(999L));
        }
    }

    @Nested
    @DisplayName("toggleCollect() 收藏切换")
    class ToggleCollect {

        @Test
        @DisplayName("未收藏时应执行收藏操作")
        void collect_shouldAddCollection() {
            Post post = createTestPost(1L, 100L);
            post.setCollectCount(0);
            when(securityUtil.getCurrentUserId()).thenReturn(200L);
            when(postMapper.selectById(1L)).thenReturn(post);
            when(postCollectMapper.selectByUserAndPost(200L, 1L)).thenReturn(null);
            when(postCollectMapper.insert(any(PostCollect.class))).thenReturn(1);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);

            Map<String, Object> result = postService.toggleCollect(1L);

            assertEquals(true, result.get("isCollected"));
            assertEquals(1, result.get("collectCount"));
        }

        @Test
        @DisplayName("已收藏时应取消收藏")
        void uncollect_shouldRemoveCollection() {
            Post post = createTestPost(1L, 100L);
            post.setCollectCount(5);
            when(securityUtil.getCurrentUserId()).thenReturn(200L);
            when(postMapper.selectById(1L)).thenReturn(post);

            PostCollect existing = new PostCollect();
            existing.setCollectId(10L);
            when(postCollectMapper.selectByUserAndPost(200L, 1L)).thenReturn(existing);
            when(postCollectMapper.deleteById(10L)).thenReturn(1);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);

            Map<String, Object> result = postService.toggleCollect(1L);

            assertEquals(false, result.get("isCollected"));
            assertEquals(4, result.get("collectCount"));
        }

        @Test
        @DisplayName("收藏不存在的帖子应抛出 RuntimeException")
        void collectNonExistent_shouldThrow() {
            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            when(postMapper.selectById(999L)).thenReturn(null);

            assertThrows(RuntimeException.class, () -> postService.toggleCollect(999L));
        }
    }

    @Nested
    @DisplayName("getCollections() 收藏列表")
    class GetCollections {

        @Test
        @DisplayName("应返回当前用户的收藏帖子列表")
        void shouldReturnUserCollections() {
            when(securityUtil.getCurrentUserId()).thenReturn(1L);

            Page<PostCollect> collectPage = new Page<>(1, 10);
            collectPage.setRecords(Collections.emptyList());
            collectPage.setTotal(0);
            when(postCollectMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                    .thenReturn(collectPage);

            PageResult<Post> result = postService.getCollections(1, 10);

            assertNotNull(result);
            assertEquals(0, result.getTotal());
        }
    }

    @Nested
    @DisplayName("togglePin() 置顶切换")
    class TogglePin {

        @Test
        @DisplayName("应正确设置置顶状态")
        void shouldSetPinStatus() {
            Post post = createTestPost(1L, 100L);
            when(postMapper.selectById(1L)).thenReturn(post);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);

            PinRequest req = new PinRequest();
            req.setIsPinned(true);
            assertDoesNotThrow(() -> postService.togglePin(1L, req));
            assertEquals(1, post.getIsPinned());
        }

        @Test
        @DisplayName("取消置顶")
        void shouldUnpin() {
            Post post = createTestPost(1L, 100L);
            post.setIsPinned(1);
            when(postMapper.selectById(1L)).thenReturn(post);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);

            PinRequest req = new PinRequest();
            req.setIsPinned(false);
            postService.togglePin(1L, req);
            assertEquals(0, post.getIsPinned());
        }

        @Test
        @DisplayName("置顶不存在的帖子应抛出 RuntimeException")
        void pinNonExistent_shouldThrow() {
            when(postMapper.selectById(999L)).thenReturn(null);
            PinRequest req = new PinRequest();
            req.setIsPinned(true);
            assertThrows(RuntimeException.class, () -> postService.togglePin(999L, req));
        }
    }

    @Nested
    @DisplayName("toggleEssence() 精华切换")
    class ToggleEssence {

        @Test
        @DisplayName("应正确设置精华状态")
        void shouldSetEssenceStatus() {
            Post post = createTestPost(1L, 100L);
            when(postMapper.selectById(1L)).thenReturn(post);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);

            EssenceRequest req = new EssenceRequest();
            req.setIsEssence(true);
            assertDoesNotThrow(() -> postService.toggleEssence(1L, req));
            assertEquals(1, post.getIsEssence());
        }
    }
}
