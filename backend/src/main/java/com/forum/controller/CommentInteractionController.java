package com.forum.controller;

import com.forum.common.PageResult;
import com.forum.common.Result;
import com.forum.entity.Comment;
import com.forum.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentInteractionController {

    private final CommentService commentService;

    @PostMapping("/comments/{commentId}/like")
    public Result<Map<String, Object>> toggleLike(@PathVariable Long commentId) {
        return Result.ok(commentService.toggleLike(commentId));
    }

    @GetMapping("/comments/{commentId}/replies")
    public Result<PageResult<Comment>> getReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(commentService.getReplies(commentId, page, pageSize));
    }

    @DeleteMapping("/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return Result.ok("已删除", null);
    }
}
