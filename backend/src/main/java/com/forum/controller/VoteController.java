package com.forum.controller;

import com.forum.common.Result;
import com.forum.dto.VoteCreateRequest;
import com.forum.dto.VoteSubmitRequest;
import com.forum.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PostMapping("/posts/{postId}/vote")
    public Result<?> createVote(@PathVariable Long postId, @Valid @RequestBody VoteCreateRequest req) {
        return Result.ok("创建成功", voteService.createVote(postId, req));
    }

    @GetMapping("/posts/{postId}/vote")
    public Result<Map<String, Object>> getVoteByPost(@PathVariable Long postId) {
        Map<String, Object> data = voteService.getVoteByPost(postId);
        return Result.ok(data);
    }

    @PostMapping("/votes/{voteId}/submit")
    public Result<?> submitVote(@PathVariable Long voteId, @Valid @RequestBody VoteSubmitRequest req) {
        Map<String, Object> data = voteService.submitVote(voteId, req);
        return Result.ok("投票成功", data);
    }
}
