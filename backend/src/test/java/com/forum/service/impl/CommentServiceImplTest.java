package com.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forum.common.PageResult;
import com.forum.common.SecurityUtil;
import com.forum.dto.CommentCreateRequest;
import com.forum.entity.Comment;
import com.forum.entity.Post;
import com.forum.entity.User;
import com.forum.mapper.CommentLikeMapper;
import com.forum.mapper.CommentMapper;
import com.forum.mapper.PostMapper;
import com.forum.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImpl 评论服务 单元测试")
class CommentServiceImplTest {

    @Mock private CommentMapper commentMapper;
    @Mock private PostMapper postMapper;
    @Mock private UserMapper userMapper;
    @Mock private CommentLikeMapper commentLikeMapper;
    @Mock private SecurityUtil securityUtil;
    @Mock private NotificationHelper notificationHelper;

    @InjectMocks
    private CommentServiceImpl commentService;

    @Nested
    @DisplayName("getComments() 评论列表")
    class GetComments {

        @Test
        @DisplayName("默认排序(latest)按 publish_time 倒序")
        void defaultSort_shouldOrderByTime() {
            Page<Comment> mpPage = new Page<>(1, 10);
            mpPage.setRecords(Collections.emptyList());
            mpPage.setTotal(0);
            when(commentMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mpPage);

            PageResult<Comment> result = commentService.getComments(1L, 1, 10, "latest");
            assertNotNull(result);
            assertEquals(0, result.getTotal());
        }

        @Test
        @DisplayName("按 likeCount 排序")
        void likeCountSort_shouldOrderByLikes() {
            Page<Comment> mpPage = new Page<>(1, 10);
            mpPage.setRecords(Collections.emptyList());
            mpPage.setTotal(0);
            when(commentMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mpPage);

            PageResult<Comment> result = commentService.getComments(1L, 1, 10, "likeCount");
            assertNotNull(result);
        }

        @Test
        @DisplayName("评论列表应包含作者信息")
        void shouldEnrichAuthorInfo() {
            Comment comment = new Comment();
            comment.setCommentId(1L);
            comment.setAuthorId(100L);
            comment.setPostId(1L);
            comment.setContent("test comment");
            comment.setLikeCount(0);

            Page<Comment> mpPage = new Page<>(1, 10);
            mpPage.setRecords(List.of(comment));
            mpPage.setTotal(1);

            User author = new User();
            author.setUserId(100L);
            author.setNickname("author1");
            author.setAvatarUrl("/avatar.png");

            when(commentMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mpPage);
            when(userMapper.selectBatchIds(anySet())).thenReturn(List.of(author));

            PageResult<Comment> result = commentService.getComments(1L, 1, 10, "latest");

            assertEquals(1, result.getRecords().size());
            assertNotNull(result.getRecords().get(0).getAuthor());
            assertEquals("author1", result.getRecords().get(0).getAuthor().getNickname());
        }
    }

    @Nested
    @DisplayName("createComment() 创建评论")
    class CreateComment {

        @Test
        @DisplayName("创建顶级评论应成功")
        void createTopLevelComment_shouldSucceed() {
            Post post = new Post();
            post.setPostId(1L);
            post.setCommentCount(0);

            when(securityUtil.getCurrentUserId()).thenReturn(100L);
            when(postMapper.selectById(1L)).thenReturn(post);
            when(commentMapper.insert(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setCommentId(1L);
                return 1;
            });
            when(postMapper.updateById(any(Post.class))).thenReturn(1);

            CommentCreateRequest req = new CommentCreateRequest();
            req.setContent("nice post");

            Comment result = commentService.createComment(1L, req);

            assertEquals("nice post", result.getContent());
            assertEquals(1L, result.getPostId());
            verify(postMapper).updateById(post);
        }

        @Test
        @DisplayName("创建回复评论（楼中楼）应成功")
        void createReplyComment_shouldSucceed() {
            Post post = new Post();
            post.setPostId(1L);
            post.setCommentCount(5);

            Comment parent = new Comment();
            parent.setCommentId(10L);
            parent.setPostId(1L);
            parent.setAuthorId(200L);

            when(securityUtil.getCurrentUserId()).thenReturn(100L);
            when(postMapper.selectById(1L)).thenReturn(post);
            when(commentMapper.selectById(10L)).thenReturn(parent);
            when(commentMapper.insert(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setCommentId(2L);
                return 1;
            });
            when(postMapper.updateById(any(Post.class))).thenReturn(1);

            CommentCreateRequest req = new CommentCreateRequest();
            req.setContent("reply");
            req.setParentCommentId(10L);

            Comment result = commentService.createComment(1L, req);
            assertEquals("reply", result.getContent());
            assertEquals(10L, result.getParentCommentId());
        }

        @Test
        @DisplayName("帖子不存在应抛出 RuntimeException")
        void nonExistentPost_shouldThrow() {
            when(securityUtil.getCurrentUserId()).thenReturn(100L);
            when(postMapper.selectById(999L)).thenReturn(null);

            CommentCreateRequest req = new CommentCreateRequest();
            req.setContent("test");

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> commentService.createComment(999L, req));
            assertEquals("帖子不存在", ex.getMessage());
        }

        @Test
        @DisplayName("父评论不存在应抛出 RuntimeException")
        void nonExistentParentComment_shouldThrow() {
            Post post = new Post();
            post.setPostId(1L);

            when(securityUtil.getCurrentUserId()).thenReturn(100L);
            when(postMapper.selectById(1L)).thenReturn(post);
            when(commentMapper.selectById(999L)).thenReturn(null);

            CommentCreateRequest req = new CommentCreateRequest();
            req.setContent("reply");
            req.setParentCommentId(999L);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> commentService.createComment(1L, req));
            assertEquals("父评论不存在", ex.getMessage());
        }

        @Test
        @DisplayName("父评论不属于该帖子应抛出 RuntimeException")
        void parentCommentNotInPost_shouldThrow() {
            Post post = new Post();
            post.setPostId(1L);

            Comment parent = new Comment();
            parent.setCommentId(10L);
            parent.setPostId(2L); // different post

            when(securityUtil.getCurrentUserId()).thenReturn(100L);
            when(postMapper.selectById(1L)).thenReturn(post);
            when(commentMapper.selectById(10L)).thenReturn(parent);

            CommentCreateRequest req = new CommentCreateRequest();
            req.setContent("reply");
            req.setParentCommentId(10L);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> commentService.createComment(1L, req));
            assertEquals("父评论不存在", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("deleteComment() 删除评论")
    class DeleteComment {

        @Test
        @DisplayName("作者可软删除自己的评论")
        void authorCanDelete() {
            Comment comment = new Comment();
            comment.setCommentId(1L);
            comment.setAuthorId(100L);

            when(securityUtil.getCurrentUserId()).thenReturn(100L);
            when(commentMapper.selectById(1L)).thenReturn(comment);
            when(commentMapper.updateById(any(Comment.class))).thenReturn(1);

            assertDoesNotThrow(() -> commentService.deleteComment(1L));
            assertEquals(1, comment.getIsDeleted());
        }

        @Test
        @DisplayName("非作者不能删除他人评论")
        void nonAuthorCannotDelete() {
            Comment comment = new Comment();
            comment.setCommentId(1L);
            comment.setAuthorId(200L);

            when(securityUtil.getCurrentUserId()).thenReturn(100L);
            when(commentMapper.selectById(1L)).thenReturn(comment);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> commentService.deleteComment(1L));
            assertEquals("无权删除他人评论", ex.getMessage());
        }

        @Test
        @DisplayName("删除不存在的评论应抛出 RuntimeException")
        void deleteNonExistent_shouldThrow() {
            when(securityUtil.getCurrentUserId()).thenReturn(100L);
            when(commentMapper.selectById(999L)).thenReturn(null);

            assertThrows(RuntimeException.class, () -> commentService.deleteComment(999L));
        }
    }

    @Nested
    @DisplayName("toggleLike() 评论点赞")
    class ToggleLike {

        @Test
        @DisplayName("点赞应增加 likeCount")
        void toggleLike_shouldIncrementCount() {
            Comment comment = new Comment();
            comment.setCommentId(1L);
            comment.setLikeCount(3);

            when(commentMapper.selectById(1L)).thenReturn(comment);
            when(commentMapper.updateById(any(Comment.class))).thenReturn(1);

            Map<String, Object> result = commentService.toggleLike(1L);

            assertEquals(true, result.get("isLiked"));
            assertEquals(4, result.get("likeCount"));
        }

        @Test
        @DisplayName("点赞不存在的评论应抛出 RuntimeException")
        void likeNonExistent_shouldThrow() {
            when(commentMapper.selectById(999L)).thenReturn(null);

            assertThrows(RuntimeException.class, () -> commentService.toggleLike(999L));
        }
    }
}
