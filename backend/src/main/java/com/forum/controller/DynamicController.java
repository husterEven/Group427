package com.forum.controller;

import com.forum.common.Result;
import com.forum.dto.DynamicCreateRequest;
import com.forum.service.DynamicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dynamics")
@RequiredArgsConstructor
public class DynamicController {

    private final DynamicService dynamicService;

    @GetMapping
    public Result<?> getFeed(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "20") int pageSize,
                             @RequestParam(defaultValue = "latest") String filter) {
        return Result.ok(dynamicService.getFeed(page, pageSize, filter));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<?> createDynamic(@Valid @RequestBody DynamicCreateRequest req) {
        return Result.ok("发布成功", dynamicService.createDynamic(req));
    }

    @DeleteMapping("/{dynamicId}")
    public Result<?> deleteDynamic(@PathVariable Long dynamicId) {
        dynamicService.deleteDynamic(dynamicId);
        return Result.ok("已删除", null);
    }

    @GetMapping("/user/{userId}")
    public Result<?> getByUser(@PathVariable Long userId) {
        return Result.ok(dynamicService.getByUser(userId));
    }
}
