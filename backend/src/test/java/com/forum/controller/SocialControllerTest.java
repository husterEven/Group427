package com.forum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.common.GlobalExceptionHandler;
import com.forum.dto.*;
import com.forum.entity.GroupInfo;
import com.forum.entity.GroupPost;
import com.forum.entity.PrivateMessage;
import com.forum.service.SocialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocialController 社交接口 单元测试")
class SocialControllerTest {

    @Mock private SocialService socialService;
    @InjectMocks private SocialController socialController;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(socialController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/users/{userId}/follow — 关注/取消")
    void toggleFollow() throws Exception {
        doNothing().when(socialService).toggleFollow(2L);
        mockMvc.perform(post("/api/v1/users/2/follow")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{userId}/star — 设置星标")
    void setStar() throws Exception {
        doNothing().when(socialService).setStar(eq(2L), any(StarRequest.class));
        mockMvc.perform(put("/api/v1/users/2/star")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isStarred\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/following — 关注列表")
    void getFollowing() throws Exception {
        when(socialService.getFollowing(1L)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/v1/users/1/following")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/followers — 粉丝列表")
    void getFollowers() throws Exception {
        when(socialService.getFollowers(1L)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/v1/users/1/followers")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/messages/conversations — 会话列表")
    void getConversations() throws Exception {
        when(socialService.getConversations()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/v1/messages/conversations")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/messages/with/{userId} — 私信记录")
    void getMessages() throws Exception {
        when(socialService.getMessages(2L)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/v1/messages/with/2")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/messages/with/{userId} — 发送私信")
    void sendMessage() throws Exception {
        PrivateMessage msg = new PrivateMessage();
        msg.setMessageId(1L);
        msg.setContent("hello");
        when(socialService.sendMessage(eq(2L), any(MessageSendRequest.class))).thenReturn(msg);
        mockMvc.perform(post("/api/v1/messages/with/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("发送成功"));
    }

    @Test
    @DisplayName("PUT /api/v1/messages/{id}/read — 标记已读")
    void markRead() throws Exception {
        doNothing().when(socialService).markRead(1L);
        mockMvc.perform(put("/api/v1/messages/1/read")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/messages/read-all — 全部已读")
    void markAllRead() throws Exception {
        doNothing().when(socialService).markAllRead();
        mockMvc.perform(put("/api/v1/messages/read-all")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/groups — 群组列表")
    void getGroups() throws Exception {
        when(socialService.getGroups()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/v1/groups")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/groups — 创建群组")
    void createGroup() throws Exception {
        GroupInfo g = new GroupInfo();
        g.setGroupId(1L);
        g.setGroupName("test");
        when(socialService.createGroup(any(GroupCreateRequest.class))).thenReturn(g);
        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupName\":\"test\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("群组创建成功"));
    }

    @Test
    @DisplayName("GET /api/v1/groups/{groupId} — 群组详情")
    void getGroupDetail() throws Exception {
        GroupInfo g = new GroupInfo();
        g.setGroupId(1L);
        g.setGroupName("test");
        when(socialService.getGroupDetail(1L)).thenReturn(g);
        mockMvc.perform(get("/api/v1/groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupName").value("test"));
    }

    @Test
    @DisplayName("PUT /api/v1/groups/{groupId} — 更新群组")
    void updateGroup() throws Exception {
        doNothing().when(socialService).updateGroup(eq(1L), any(GroupUpdateRequest.class));
        mockMvc.perform(put("/api/v1/groups/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupName\":\"updated\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/groups/{groupId}/join — 加入群组")
    void joinGroup() throws Exception {
        doNothing().when(socialService).joinGroup(1L);
        mockMvc.perform(post("/api/v1/groups/1/join"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("已加入"));
    }

    @Test
    @DisplayName("POST /api/v1/groups/{groupId}/leave — 退出群组")
    void leaveGroup() throws Exception {
        doNothing().when(socialService).leaveGroup(1L);
        mockMvc.perform(post("/api/v1/groups/1/leave"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("已退出"));
    }

    @Test
    @DisplayName("GET /api/v1/groups/{groupId}/members — 群成员")
    void getMembers() throws Exception {
        when(socialService.getMembers(1L)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/v1/groups/1/members")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/groups/{groupId}/members/{userId}/role — 设置角色")
    void setRole() throws Exception {
        doNothing().when(socialService).setRole(1L, 2L, 1);
        mockMvc.perform(put("/api/v1/groups/1/members/2/role").param("role", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/groups/{groupId}/members/{userId} — 踢出成员")
    void kickMember() throws Exception {
        doNothing().when(socialService).kickMember(1L, 2L);
        mockMvc.perform(delete("/api/v1/groups/1/members/2")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/groups/{groupId}/posts — 群组帖子")
    void getGroupPosts() throws Exception {
        when(socialService.getGroupPosts(1L)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/v1/groups/1/posts")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/groups/{groupId}/posts — 创建群组帖子")
    void createGroupPost() throws Exception {
        GroupPost p = new GroupPost();
        p.setGroupPostId(1L);
        p.setContent("hello");
        when(socialService.createGroupPost(eq(1L), any(DynamicCreateRequest.class))).thenReturn(p);
        mockMvc.perform(post("/api/v1/groups/1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("发布成功"));
    }

    @Test
    @DisplayName("DELETE /api/v1/groups/{groupId}/posts/{postId} — 删除群组帖子")
    void deleteGroupPost() throws Exception {
        doNothing().when(socialService).deleteGroupPost(1L);
        mockMvc.perform(delete("/api/v1/groups/1/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("已删除"));
    }
}
