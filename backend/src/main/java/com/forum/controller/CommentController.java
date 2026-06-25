package com.forum.controller;

import com.forum.common.PageResult;
import com.forum.common.Result;
import com.forum.dto.CommentCreateRequest;
import com.forum.entity.Comment;
import com.forum.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public Result<PageResult<Comment>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "latest") String sort) {
        PageResult<Comment> pageResult = commentService.getComments(postId, page, pageSize, sort);
        return Result.ok(pageResult);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<Comment> createComment(@PathVariable Long postId, @Valid @RequestBody CommentCreateRequest req) {
        Comment comment = commentService.createComment(postId, req);
        return Result.ok("评论成功", comment);
    }

    @DeleteMapping("/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long postId, @PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return Result.ok("已删除", null);
    }
}
