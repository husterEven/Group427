package com.forum.controller;

import com.forum.common.PageResult;
import com.forum.common.Result;
import com.forum.dto.EssenceRequest;
import com.forum.dto.PinRequest;
import com.forum.dto.PostCreateRequest;
import com.forum.dto.PostQueryRequest;
import com.forum.dto.PostUpdateRequest;
import com.forum.entity.Post;
import com.forum.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public Result<PageResult<Post>> getPostList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) Integer sectionId,
            @RequestParam(required = false) Integer zoneId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isEssence,
            @RequestParam(required = false) Long authorId) {

        PostQueryRequest req = new PostQueryRequest();
        req.setPage(page);
        req.setPageSize(pageSize);
        req.setSort(sort);
        req.setSectionId(sectionId);
        req.setZoneId(zoneId);
        req.setKeyword(keyword);
        req.setIsEssence(isEssence);
        req.setAuthorId(authorId);

        PageResult<Post> pageResult = postService.getPostList(req);
        return Result.ok(pageResult);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<Post> createPost(@Valid @RequestBody PostCreateRequest req) {
        Post post = postService.createPost(req);
        return Result.ok("发布成功", post);
    }

    @GetMapping("/{postId}")
    public Result<Post> getPostDetail(@PathVariable Long postId) {
        Post post = postService.getPostDetail(postId);
        return Result.ok(post);
    }

    @PutMapping("/{postId}")
    public Result<Post> updatePost(@PathVariable Long postId, @Valid @RequestBody PostUpdateRequest req) {
        Post post = postService.updatePost(postId, req);
        return Result.ok("更新成功", post);
    }

    @DeleteMapping("/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return Result.ok("已删除", null);
    }

    @PostMapping("/{postId}/like")
    public Result<Map<String, Object>> toggleLike(@PathVariable Long postId) {
        Map<String, Object> result = postService.toggleLike(postId);
        return Result.ok(result);
    }

    @PostMapping("/{postId}/collect")
    public Result<Map<String, Object>> toggleCollect(@PathVariable Long postId) {
        Map<String, Object> result = postService.toggleCollect(postId);
        return Result.ok(result);
    }

    @GetMapping("/collections")
    public Result<PageResult<Post>> getCollections(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<Post> pageResult = postService.getCollections(page, pageSize);
        return Result.ok(pageResult);
    }

    @PutMapping("/{postId}/pin")
    public Result<Void> togglePin(@PathVariable Long postId, @Valid @RequestBody PinRequest req) {
        postService.togglePin(postId, req);
        return Result.ok(req.getIsPinned() ? "已置顶" : "已取消置顶", null);
    }

    @PutMapping("/{postId}/essence")
    public Result<Void> toggleEssence(@PathVariable Long postId, @Valid @RequestBody EssenceRequest req) {
        postService.toggleEssence(postId, req);
        return Result.ok("操作成功", null);
    }
}
