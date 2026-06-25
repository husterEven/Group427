package com.forum.service;

import com.forum.common.PageResult;
import com.forum.dto.CommentCreateRequest;
import com.forum.entity.Comment;

import java.util.Map;

public interface CommentService {

    PageResult<Comment> getComments(Long postId, int page, int pageSize, String sort);

    Comment createComment(Long postId, CommentCreateRequest req);

    void deleteComment(Long commentId);

    Map<String, Object> toggleLike(Long commentId);

    PageResult<Comment> getReplies(Long commentId, int page, int pageSize);
}
