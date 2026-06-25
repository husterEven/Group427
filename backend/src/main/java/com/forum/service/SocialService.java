package com.forum.service;

import com.forum.dto.*;
import com.forum.entity.*;

import java.util.List;
import java.util.Map;

public interface SocialService {

    void toggleFollow(Long followeeId);

    void setStar(Long followeeId, StarRequest req);

    List<Follow> getFollowing(Long userId);

    List<Follow> getFollowers(Long userId);

    List<Map<String, Object>> getConversations();

    List<PrivateMessage> getMessages(Long targetUserId);

    PrivateMessage sendMessage(Long targetUserId, MessageSendRequest req);

    int getUnreadDmCount();

    void markRead(Long targetUserId);

    void markAllRead();

    List<GroupInfo> getGroups();

    GroupInfo createGroup(GroupCreateRequest req);

    GroupInfo getGroupDetail(Long groupId);

    void updateGroup(Long groupId, GroupUpdateRequest req);

    void joinGroup(Long groupId);

    void leaveGroup(Long groupId);

    List<GroupMember> getMembers(Long groupId);

    void setRole(Long groupId, Long memberUserId, int role);

    void kickMember(Long groupId, Long memberUserId);

    void inviteMember(Long groupId, Long userId);

    List<GroupPost> getGroupPosts(Long groupId);

    GroupPost createGroupPost(Long groupId, DynamicCreateRequest req);

    void deleteGroupPost(Long groupPostId);
}
