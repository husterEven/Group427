package com.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forum.common.PageResult;
import com.forum.common.SecurityUtil;
import com.forum.dto.CommentCreateRequest;
import com.forum.entity.Comment;
import com.forum.entity.CommentLike;
import com.forum.entity.Post;
import com.forum.entity.User;
import com.forum.mapper.CommentLikeMapper;
import com.forum.mapper.CommentMapper;
import com.forum.mapper.PostMapper;
import com.forum.mapper.UserMapper;
import com.forum.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final SecurityUtil securityUtil;
    private final NotificationHelper notificationHelper;

    @Override
    public PageResult<Comment> getComments(Long postId, int page, int pageSize, String sort) {
        Page<Comment> p = new Page<>(page, pageSize);
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("post_id", postId);
        wrapper.isNull("parent_comment_id");
        if ("hot".equals(sort)) {
            wrapper.orderByDesc("like_count");
        } else {
            wrapper.orderByDesc("publish_time");
        }
        Page<Comment> result = commentMapper.selectPage(p, wrapper);
        enrichComments(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(),
                (int) result.getCurrent(), (int) result.getSize());
    }

    private void enrichComments(List<Comment> comments) {
        if (comments.isEmpty()) return;
        Long currentUserId = securityUtil.getCurrentUserIdOrNull();

        Set<Long> authorIds = comments.stream().map(Comment::getAuthorId).collect(Collectors.toSet());
        Set<Long> commentIds = comments.stream().map(Comment::getCommentId).collect(Collectors.toSet());

        Map<Long, User> userMap = userMapper.selectBatchIds(authorIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        Map<Long, Integer> replyCountMap = new HashMap<>();
        Map<Long, Boolean> likedMap = new HashMap<>();

        if (!commentIds.isEmpty()) {
            QueryWrapper<Comment> replyWrapper = new QueryWrapper<>();
            replyWrapper.in("parent_comment_id", commentIds);
            replyWrapper.eq("is_deleted", 0);
            List<Comment> replies = commentMapper.selectList(replyWrapper);
            replyCountMap = replies.stream()
                    .collect(Collectors.groupingBy(Comment::getParentCommentId, Collectors.summingInt(c -> 1)));

            if (currentUserId != null) {
                QueryWrapper<CommentLike> likeWrapper = new QueryWrapper<>();
                likeWrapper.in("comment_id", commentIds).eq("user_id", currentUserId);
                likedMap = commentLikeMapper.selectList(likeWrapper).stream()
                        .collect(Collectors.toMap(CommentLike::getCommentId, l -> true));
            }
        }

        for (Comment comment : comments) {
            User user = userMap.get(comment.getAuthorId());
            if (user != null) {
                User brief = new User();
                brief.setUserId(user.getUserId());
                brief.setNickname(user.getNickname());
                brief.setAvatarUrl(user.getAvatarUrl());
                comment.setAuthor(brief);
            }
            comment.setReplyCount(replyCountMap.getOrDefault(comment.getCommentId(), 0));
            comment.setIsLiked(likedMap.containsKey(comment.getCommentId()));
        }
    }

    @Override
    public Comment createComment(Long postId, CommentCreateRequest req) {
        Long userId = securityUtil.getCurrentUserId();
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        if (req.getParentCommentId() != null) {
            Comment parent = commentMapper.selectById(req.getParentCommentId());
            if (parent == null || !parent.getPostId().equals(postId)) {
                throw new RuntimeException("父评论不存在");
            }
        }
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setParentCommentId(req.getParentCommentId());
        comment.setAuthorId(userId);
        comment.setContent(req.getContent());
        comment.setLikeCount(0);
        comment.setAuditStatus(1);
        comment.setPublishTime(LocalDateTime.now());
        commentMapper.insert(comment);
        post.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(post);

        if (!userId.equals(post.getAuthorId())) {
            User currentUser = userMapper.selectById(userId);
            String commenterName = currentUser != null ? currentUser.getNickname() : "用户";
            notificationHelper.createNotification(
                post.getAuthorId(), 2, "帖子收到新评论",
                commenterName + " 评论了你的帖子", 0, postId
            );
        }
        if (req.getParentCommentId() != null) {
            Comment parentComment = commentMapper.selectById(req.getParentCommentId());
            if (parentComment != null && !userId.equals(parentComment.getAuthorId())
                    && !parentComment.getAuthorId().equals(post.getAuthorId())) {
                User currentUser = userMapper.selectById(userId);
                String replierName = currentUser != null ? currentUser.getNickname() : "用户";
                notificationHelper.createNotification(
                    parentComment.getAuthorId(), 2, "评论收到回复",
                    replierName + " 回复了你的评论", 0, postId
                );
            }
        }

        User author = userMapper.selectById(userId);
        if (author != null) {
            User brief = new User();
            brief.setUserId(author.getUserId());
            brief.setNickname(author.getNickname());
            brief.setAvatarUrl(author.getAvatarUrl());
            comment.setAuthor(brief);
        }
        comment.setReplyCount(0);
        comment.setIsLiked(false);
        return comment;
    }

    @Override
    public void deleteComment(Long commentId) {
        Long userId = securityUtil.getCurrentUserId();
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        if (!comment.getAuthorId().equals(userId)) {
            throw new RuntimeException("无权删除他人评论");
        }
        comment.setIsDeleted(1);
        commentMapper.updateById(comment);
    }

    @Override
    public Map<String, Object> toggleLike(Long commentId) {
        Long userId = securityUtil.getCurrentUserId();
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        CommentLike existing = commentLikeMapper.selectByUserAndComment(userId, commentId);
        Map<String, Object> result = new HashMap<>();
        if (existing != null) {
            commentLikeMapper.deleteById(existing.getLikeId());
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            commentMapper.updateById(comment);
            result.put("isLiked", false);
        } else {
            CommentLike like = new CommentLike();
            like.setUserId(userId);
            like.setCommentId(commentId);
            like.setCreatedAt(LocalDateTime.now());
            commentLikeMapper.insert(like);
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentMapper.updateById(comment);
            result.put("isLiked", true);
            if (!userId.equals(comment.getAuthorId())) {
                User currentUser = userMapper.selectById(userId);
                String likerName = currentUser != null ? currentUser.getNickname() : "用户";
                notificationHelper.createNotification(
                    comment.getAuthorId(), 1, "评论被点赞",
                    likerName + " 点赞了你的评论", 0, comment.getPostId()
                );
            }
        }
        result.put("likeCount", comment.getLikeCount());
        return result;
    }

    @Override
    public PageResult<Comment> getReplies(Long commentId, int page, int pageSize) {
        Page<Comment> p = new Page<>(page, pageSize);
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_comment_id", commentId);
        wrapper.orderByAsc("publish_time");
        Page<Comment> result = commentMapper.selectPage(p, wrapper);
        enrichComments(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(),
                (int) result.getCurrent(), (int) result.getSize());
    }
}
