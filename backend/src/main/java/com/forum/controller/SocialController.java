package com.forum.controller;

import com.forum.common.Result;
import com.forum.dto.*;
import com.forum.service.SocialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;

    @PostMapping("/users/{userId}/follow")
    public Result<?> toggleFollow(@PathVariable Long userId) {
        socialService.toggleFollow(userId);
        return Result.ok(null);
    }

    @PutMapping("/users/{userId}/star")
    public Result<?> setStar(@PathVariable Long userId, @Valid @RequestBody StarRequest req) {
        socialService.setStar(userId, req);
        return Result.ok(null);
    }

    @GetMapping("/users/{userId}/following")
    public Result<?> getFollowing(@PathVariable Long userId) {
        return Result.ok(socialService.getFollowing(userId));
    }

    @GetMapping("/users/{userId}/followers")
    public Result<?> getFollowers(@PathVariable Long userId) {
        return Result.ok(socialService.getFollowers(userId));
    }

    @GetMapping("/messages/unread-count")
    public Result<?> getUnreadDmCount() {
        return Result.ok(Map.of("total", socialService.getUnreadDmCount()));
    }

    @GetMapping("/messages/conversations")
    public Result<?> getConversations() {
        return Result.ok(socialService.getConversations());
    }

    @GetMapping("/messages/with/{userId}")
    public Result<?> getMessages(@PathVariable Long userId) {
        return Result.ok(socialService.getMessages(userId));
    }

    @PostMapping("/messages/with/{userId}")
    public Result<?> sendMessage(@PathVariable Long userId, @Valid @RequestBody MessageSendRequest req) {
        return Result.ok("发送成功", socialService.sendMessage(userId, req));
    }

    @PutMapping("/messages/{messageId}/read")
    public Result<?> markRead(@PathVariable Long messageId) {
        socialService.markRead(messageId);
        return Result.ok(null);
    }

    @PutMapping("/messages/read-all")
    public Result<?> markAllRead() {
        socialService.markAllRead();
        return Result.ok(null);
    }

    @GetMapping("/groups")
    public Result<?> getGroups() {
        return Result.ok(socialService.getGroups());
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<?> createGroup(@Valid @RequestBody GroupCreateRequest req) {
        return Result.ok("群组创建成功", socialService.createGroup(req));
    }

    @GetMapping("/groups/{groupId}")
    public Result<?> getGroupDetail(@PathVariable Long groupId) {
        return Result.ok(socialService.getGroupDetail(groupId));
    }

    @PutMapping("/groups/{groupId}")
    public Result<?> updateGroup(@PathVariable Long groupId, @Valid @RequestBody GroupUpdateRequest req) {
        socialService.updateGroup(groupId, req);
        return Result.ok(null);
    }

    @PostMapping("/groups/{groupId}/join")
    public Result<?> joinGroup(@PathVariable Long groupId) {
        socialService.joinGroup(groupId);
        return Result.ok("已加入", null);
    }

    @PostMapping("/groups/{groupId}/leave")
    public Result<?> leaveGroup(@PathVariable Long groupId) {
        socialService.leaveGroup(groupId);
        return Result.ok("已退出", null);
    }

    @GetMapping("/groups/{groupId}/members")
    public Result<?> getMembers(@PathVariable Long groupId) {
        return Result.ok(socialService.getMembers(groupId));
    }

    @PutMapping("/groups/{groupId}/members/{userId}/role")
    public Result<?> setRole(@PathVariable Long groupId,
                             @PathVariable Long userId,
                             @RequestBody Map<String, Integer> body) {
        socialService.setRole(groupId, userId, body.get("role"));
        return Result.ok(null);
    }

    @PostMapping("/groups/{groupId}/members/{userId}/kick")
    public Result<?> kickMember(@PathVariable Long groupId, @PathVariable Long userId) {
        socialService.kickMember(groupId, userId);
        return Result.ok(null);
    }

    @PostMapping("/groups/{groupId}/invite/{userId}")
    public Result<?> inviteMember(@PathVariable Long groupId, @PathVariable Long userId) {
        socialService.inviteMember(groupId, userId);
        return Result.ok("邀请成功", null);
    }

    @GetMapping("/groups/{groupId}/posts")
    public Result<?> getGroupPosts(@PathVariable Long groupId) {
        return Result.ok(socialService.getGroupPosts(groupId));
    }

    @PostMapping("/groups/{groupId}/posts")
    public Result<?> createGroupPost(@PathVariable Long groupId, @Valid @RequestBody DynamicCreateRequest req) {
        return Result.ok("发布成功", socialService.createGroupPost(groupId, req));
    }

    @DeleteMapping("/groups/{groupId}/posts/{postId}")
    public Result<?> deleteGroupPost(@PathVariable Long groupId, @PathVariable Long postId) {
        socialService.deleteGroupPost(postId);
        return Result.ok("已删除", null);
    }
}
