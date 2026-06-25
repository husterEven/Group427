package com.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.forum.common.SecurityUtil;
import com.forum.dto.*;
import com.forum.entity.*;
import com.forum.mapper.*;
import com.forum.service.SocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SocialServiceImpl implements SocialService {

    private final FollowMapper followMapper;
    private final PrivateMessageMapper privateMessageMapper;
    private final GroupInfoMapper groupInfoMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupPostMapper groupPostMapper;
    private final UserMapper userMapper;
    private final SecurityUtil securityUtil;
    private final NotificationHelper notificationHelper;

    @Override
    public void toggleFollow(Long followeeId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId.equals(followeeId)) {
            throw new RuntimeException("不能关注自己");
        }
        Follow existing = followMapper.selectByBoth(currentUserId, followeeId);
        if (existing != null) {
            followMapper.deleteById(existing.getRelationId());
        } else {
            Follow follow = new Follow();
            follow.setFollowerId(currentUserId);
            follow.setFolloweeId(followeeId);
            follow.setIsStarred(0);
            follow.setCreatedAt(LocalDateTime.now());
            followMapper.insert(follow);
            User currentUser = userMapper.selectById(currentUserId);
            String followerName = currentUser != null ? currentUser.getNickname() : "用户";
            notificationHelper.createNotification(
                followeeId, 3, "新的关注",
                followerName + " 关注了你", 2, currentUserId
            );
        }
    }

    @Override
    public void setStar(Long followeeId, StarRequest req) {
        Long currentUserId = securityUtil.getCurrentUserId();
        Follow follow = followMapper.selectByBoth(currentUserId, followeeId);
        if (follow == null) {
            throw new RuntimeException("未关注该用户");
        }
        follow.setIsStarred(req.getIsStarred() ? 1 : 0);
        followMapper.updateById(follow);
    }

    @Override
    public List<Follow> getFollowing(Long userId) {
        List<Follow> follows = followMapper.selectByFollowerId(userId);
        enrichFollows(follows, true, userId);
        return follows;
    }

    @Override
    public List<Follow> getFollowers(Long userId) {
        List<Follow> follows = followMapper.selectByFolloweeId(userId);
        enrichFollows(follows, false, userId);
        return follows;
    }

    private void enrichFollows(List<Follow> follows, boolean isFollowing, Long currentUserId) {
        if (follows.isEmpty()) return;
        Set<Long> targetIds = follows.stream()
                .map(f -> isFollowing ? f.getFolloweeId() : f.getFollowerId())
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(targetIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));
        Set<Long> mutualIds = Set.of();
        if (isFollowing) {
            List<Follow> reverseFollows = followMapper.selectByFolloweeId(currentUserId);
            mutualIds = reverseFollows.stream().map(Follow::getFollowerId).collect(Collectors.toSet());
        }
        for (Follow f : follows) {
            Long targetId = isFollowing ? f.getFolloweeId() : f.getFollowerId();
            User u = userMap.get(targetId);
            if (u != null) {
                f.setUserId(u.getUserId());
                f.setNickname(u.getNickname());
                f.setAvatarUrl(u.getAvatarUrl());
                f.setBio(u.getBio());
                f.setIsMutual(mutualIds.contains(u.getUserId()));
            }
        }
    }

    @Override
    public List<Map<String, Object>> getConversations() {
        Long currentUserId = securityUtil.getCurrentUserId();
        QueryWrapper<PrivateMessage> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w.eq("sender_id", currentUserId).or().eq("receiver_id", currentUserId))
               .eq("is_deleted", 0)
               .orderByDesc("send_time");
        List<PrivateMessage> messages = privateMessageMapper.selectList(wrapper);

        Map<Long, List<PrivateMessage>> grouped = new LinkedHashMap<>();
        for (PrivateMessage m : messages) {
            Long partnerId = m.getSenderId().equals(currentUserId) ? m.getReceiverId() : m.getSenderId();
            grouped.computeIfAbsent(partnerId, k -> new ArrayList<>()).add(m);
        }

        Set<Long> partnerIds = grouped.keySet();
        Map<Long, User> userMap = userMapper.selectBatchIds(partnerIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        List<Map<String, Object>> conversations = new ArrayList<>();
        for (Map.Entry<Long, List<PrivateMessage>> entry : grouped.entrySet()) {
            Long partnerId = entry.getKey();
            List<PrivateMessage> msgs = entry.getValue();
            PrivateMessage lastMsg = msgs.get(0);
            long unread = msgs.stream().filter(m -> m.getReceiverId().equals(currentUserId) && m.getIsRead() == 0).count();

            Map<String, Object> conv = new HashMap<>();
            User partner = userMap.get(partnerId);
            if (partner != null) {
                Map<String, Object> targetUser = new HashMap<>();
                targetUser.put("userId", partner.getUserId());
                targetUser.put("nickname", partner.getNickname());
                targetUser.put("avatarUrl", partner.getAvatarUrl());
                conv.put("targetUser", targetUser);
            }
            conv.put("lastMessage", lastMsg.getContent());
            conv.put("lastTime", lastMsg.getSendTime().toString());
            conv.put("unreadCount", (int) unread);
            conversations.add(conv);
        }
        return conversations;
    }

    @Override
    public List<PrivateMessage> getMessages(Long targetUserId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        List<PrivateMessage> messages = privateMessageMapper.selectConversation(currentUserId, targetUserId);
        for (PrivateMessage m : messages) {
            m.setIsMine(m.getSenderId().equals(currentUserId));
        }
        return messages;
    }

    @Override
    public PrivateMessage sendMessage(Long targetUserId, MessageSendRequest req) {
        Long currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId.equals(targetUserId)) {
            throw new RuntimeException("不能给自己发消息");
        }
        PrivateMessage message = new PrivateMessage();
        message.setSenderId(currentUserId);
        message.setReceiverId(targetUserId);
        message.setContent(req.getContent());
        message.setIsRead(0);
        message.setSendTime(LocalDateTime.now());
        privateMessageMapper.insert(message);

        User sender = userMapper.selectById(currentUserId);
        String senderName = sender != null ? sender.getNickname() : "用户";
        notificationHelper.createNotification(
            targetUserId, 4, "新的私信",
            senderName + " 发来一条私信", 2, currentUserId
        );

        return message;
    }

    @Override
    public int getUnreadDmCount() {
        Long currentUserId = securityUtil.getCurrentUserId();
        QueryWrapper<PrivateMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("receiver_id", currentUserId)
               .eq("is_read", 0)
               .eq("is_deleted", 0);
        return privateMessageMapper.selectCount(wrapper).intValue();
    }

    @Override
    public void markRead(Long targetUserId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        PrivateMessage update = new PrivateMessage();
        update.setIsRead(1);
        QueryWrapper<PrivateMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("sender_id", targetUserId)
               .eq("receiver_id", currentUserId)
               .eq("is_read", 0);
        privateMessageMapper.update(update, wrapper);
    }

    @Override
    public void markAllRead() {
        Long currentUserId = securityUtil.getCurrentUserId();
        PrivateMessage update = new PrivateMessage();
        update.setIsRead(1);
        QueryWrapper<PrivateMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("receiver_id", currentUserId)
               .eq("is_read", 0);
        privateMessageMapper.update(update, wrapper);
    }

    @Override
    public List<GroupInfo> getGroups() {
        Long currentUserId = securityUtil.getCurrentUserId();
        QueryWrapper<GroupMember> memberWrapper = new QueryWrapper<>();
        memberWrapper.select("group_id, role").eq("user_id", currentUserId);
        List<GroupMember> myMemberships = groupMemberMapper.selectList(memberWrapper);
        if (myMemberships.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> roleMap = myMemberships.stream()
                .collect(Collectors.toMap(GroupMember::getGroupId, GroupMember::getRole));

        Set<Long> groupIds = roleMap.keySet();
        QueryWrapper<GroupInfo> wrapper = new QueryWrapper<>();
        wrapper.in("group_id", groupIds).orderByDesc("created_at");
        List<GroupInfo> groups = groupInfoMapper.selectList(wrapper);

        enrichGroups(groups, roleMap);
        return groups;
    }

    private void enrichGroups(List<GroupInfo> groups, Map<Long, Integer> roleMap) {
        if (groups.isEmpty()) return;
        Set<Long> groupIds = groups.stream().map(GroupInfo::getGroupId).collect(Collectors.toSet());
        Set<Long> ownerIds = groups.stream().map(GroupInfo::getOwnerId).collect(Collectors.toSet());

        Map<Long, User> userMap = userMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        QueryWrapper<GroupMember> countWrapper = new QueryWrapper<>();
        countWrapper.in("group_id", groupIds);
        List<GroupMember> allMembers = groupMemberMapper.selectList(countWrapper);
        Map<Long, Long> memberCountMap = allMembers.stream()
                .collect(Collectors.groupingBy(GroupMember::getGroupId, Collectors.counting()));

        for (GroupInfo group : groups) {
            User owner = userMap.get(group.getOwnerId());
            if (owner != null) {
                Map<String, Object> ownerBrief = new HashMap<>();
                ownerBrief.put("userId", owner.getUserId());
                ownerBrief.put("nickname", owner.getNickname());
                ownerBrief.put("avatarUrl", owner.getAvatarUrl());
                group.setOwner(ownerBrief);
            }
            group.setMemberCount(memberCountMap.getOrDefault(group.getGroupId(), 0L).intValue());
            group.setMyRole(roleMap.getOrDefault(group.getGroupId(), null));
        }
    }

    @Override
    @Transactional
    public GroupInfo createGroup(GroupCreateRequest req) {
        Long currentUserId = securityUtil.getCurrentUserId();
        GroupInfo group = new GroupInfo();
        group.setGroupName(req.getGroupName());
        group.setOwnerId(currentUserId);
        group.setMode(req.getMode() != null ? req.getMode() : 0);
        group.setStatus(1);
        group.setCreatedAt(LocalDateTime.now());
        groupInfoMapper.insert(group);

        GroupMember member = new GroupMember();
        member.setGroupId(group.getGroupId());
        member.setUserId(currentUserId);
        member.setRole(2);
        member.setJoinedAt(LocalDateTime.now());
        groupMemberMapper.insert(member);

        group.setMemberCount(1);
        group.setMyRole(2);
        return group;
    }

    @Override
    public GroupInfo getGroupDetail(Long groupId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        GroupInfo group = groupInfoMapper.selectById(groupId);
        if (group == null) {
            throw new RuntimeException("圈子不存在");
        }

        QueryWrapper<GroupMember> memberWrapper = new QueryWrapper<>();
        memberWrapper.eq("group_id", groupId);
        List<GroupMember> allMembers = groupMemberMapper.selectList(memberWrapper);
        GroupMember myMembership = allMembers.stream()
                .filter(m -> m.getUserId().equals(currentUserId))
                .findFirst().orElse(null);

        Map<Long, Integer> roleMap = new HashMap<>();
        if (myMembership != null) {
            roleMap.put(groupId, myMembership.getRole());
        }
        enrichGroups(List.of(group), roleMap);
        return group;
    }

    @Override
    public void updateGroup(Long groupId, GroupUpdateRequest req) {
        Long currentUserId = securityUtil.getCurrentUserId();
        GroupInfo group = groupInfoMapper.selectById(groupId);
        if (group == null) {
            throw new RuntimeException("圈子不存在");
        }
        if (!group.getOwnerId().equals(currentUserId)) {
            throw new RuntimeException("只有圈主才能修改圈子信息");
        }
        if (req.getGroupName() != null) {
            group.setGroupName(req.getGroupName());
        }
        if (req.getMode() != null) {
            group.setMode(req.getMode());
        }
        if (req.getStatus() != null) {
            group.setStatus(req.getStatus());
        }
        groupInfoMapper.updateById(group);
    }

    @Override
    public void joinGroup(Long groupId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        GroupInfo group = groupInfoMapper.selectById(groupId);
        if (group == null) {
            throw new RuntimeException("圈子不存在");
        }
        QueryWrapper<GroupMember> memberWrapper = new QueryWrapper<>();
        memberWrapper.eq("group_id", groupId).eq("user_id", currentUserId);
        if (groupMemberMapper.selectCount(memberWrapper) > 0) {
            throw new RuntimeException("已经是圈子成员");
        }
        if (group.getMode() == 1) {
            throw new RuntimeException("该圈子需要审核才能加入");
        }
        if (group.getMode() == 2) {
            throw new RuntimeException("该圈子禁止加入");
        }
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(currentUserId);
        member.setRole(0);
        member.setJoinedAt(LocalDateTime.now());
        groupMemberMapper.insert(member);
    }

    @Override
    @Transactional
    public void leaveGroup(Long groupId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        GroupInfo group = groupInfoMapper.selectById(groupId);
        if (group == null) {
            throw new RuntimeException("圈子不存在");
        }
        QueryWrapper<GroupMember> memberWrapper = new QueryWrapper<>();
        memberWrapper.eq("group_id", groupId).eq("user_id", currentUserId);
        GroupMember member = groupMemberMapper.selectOne(memberWrapper);
        if (member == null) {
            throw new RuntimeException("不是圈子成员");
        }
        groupMemberMapper.deleteById(member.getMemberId());

        if (group.getOwnerId().equals(currentUserId)) {
            QueryWrapper<GroupMember> remainingWrapper = new QueryWrapper<>();
            remainingWrapper.eq("group_id", groupId)
                           .orderByAsc("joined_at");
            List<GroupMember> remaining = groupMemberMapper.selectList(remainingWrapper);
            if (!remaining.isEmpty()) {
                GroupMember newOwner = remaining.get(0);
                newOwner.setRole(2);
                groupMemberMapper.updateById(newOwner);
                group.setOwnerId(newOwner.getUserId());
                groupInfoMapper.updateById(group);
            } else {
                groupInfoMapper.deleteById(groupId);
            }
        }
    }

    @Override
    public List<GroupMember> getMembers(Long groupId) {
        QueryWrapper<GroupMember> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId).orderByDesc("role").orderByAsc("joined_at");
        List<GroupMember> members = groupMemberMapper.selectList(wrapper);
        if (!members.isEmpty()) {
            Set<Long> userIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toSet());
            Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getUserId, u -> u));
            for (GroupMember m : members) {
                User u = userMap.get(m.getUserId());
                if (u != null) {
                    m.setNickname(u.getNickname());
                    m.setAvatarUrl(u.getAvatarUrl());
                }
            }
        }
        return members;
    }

    @Override
    public void setRole(Long groupId, Long memberUserId, int role) {
        Long currentUserId = securityUtil.getCurrentUserId();
        GroupInfo group = groupInfoMapper.selectById(groupId);
        if (group == null) {
            throw new RuntimeException("圈子不存在");
        }
        QueryWrapper<GroupMember> ownerWrapper = new QueryWrapper<>();
        ownerWrapper.eq("group_id", groupId).eq("user_id", currentUserId);
        GroupMember currentMember = groupMemberMapper.selectOne(ownerWrapper);
        if (currentMember == null || currentMember.getRole() < 1) {
            throw new RuntimeException("无权设置角色");
        }
        QueryWrapper<GroupMember> targetWrapper = new QueryWrapper<>();
        targetWrapper.eq("group_id", groupId).eq("user_id", memberUserId);
        GroupMember targetMember = groupMemberMapper.selectOne(targetWrapper);
        if (targetMember == null) {
            throw new RuntimeException("目标成员不存在");
        }
        int oldRole = targetMember.getRole() != null ? targetMember.getRole() : 0;
        targetMember.setRole(role);
        groupMemberMapper.updateById(targetMember);

        if (role == 1 && oldRole != 1) {
            notificationHelper.createNotification(
                memberUserId, 0, "你已成为群组管理员",
                "你在群组「" + group.getGroupName() + "」中被设置为管理员", 3, groupId
            );
        } else if (role == 0 && oldRole == 1) {
            notificationHelper.createNotification(
                memberUserId, 0, "你已被撤销群组管理员",
                "你在群组「" + group.getGroupName() + "」中被撤销管理员", 3, groupId
            );
        }
    }

    @Override
    public void kickMember(Long groupId, Long memberUserId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        GroupInfo group = groupInfoMapper.selectById(groupId);
        if (group == null) {
            throw new RuntimeException("圈子不存在");
        }
        if (!group.getOwnerId().equals(currentUserId)) {
            throw new RuntimeException("只有圈主才能踢人");
        }
        if (memberUserId.equals(group.getOwnerId())) {
            throw new RuntimeException("不能踢出圈主");
        }
        QueryWrapper<GroupMember> targetWrapper = new QueryWrapper<>();
        targetWrapper.eq("group_id", groupId).eq("user_id", memberUserId);
        GroupMember targetMember = groupMemberMapper.selectOne(targetWrapper);
        if (targetMember == null) {
            throw new RuntimeException("目标成员不在圈内");
        }
        groupMemberMapper.deleteById(targetMember.getMemberId());
    }

    @Override
    public void inviteMember(Long groupId, Long userId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        GroupInfo group = groupInfoMapper.selectById(groupId);
        if (group == null) {
            throw new RuntimeException("圈子不存在");
        }
        QueryWrapper<GroupMember> selfWrapper = new QueryWrapper<>();
        selfWrapper.eq("group_id", groupId).eq("user_id", currentUserId);
        if (groupMemberMapper.selectCount(selfWrapper) == 0) {
            throw new RuntimeException("您不是圈内成员，无权邀请");
        }
        QueryWrapper<GroupMember> targetWrapper = new QueryWrapper<>();
        targetWrapper.eq("group_id", groupId).eq("user_id", userId);
        if (groupMemberMapper.selectCount(targetWrapper) > 0) {
            throw new RuntimeException("该用户已在圈内");
        }
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(0);
        member.setJoinedAt(LocalDateTime.now());
        groupMemberMapper.insert(member);

        User inviter = userMapper.selectById(currentUserId);
        String inviterName = inviter != null ? inviter.getNickname() : "用户";
        notificationHelper.createNotification(
            userId, 0, "你被邀请加入群组",
            inviterName + " 邀请你加入群组「" + group.getGroupName() + "」", 3, groupId
        );
    }

    @Override
    public List<GroupPost> getGroupPosts(Long groupId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        QueryWrapper<GroupPost> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId).orderByDesc("publish_time");
        List<GroupPost> posts = groupPostMapper.selectList(wrapper);
        if (!posts.isEmpty()) {
            Set<Long> authorIds = posts.stream().map(GroupPost::getAuthorId).collect(Collectors.toSet());
            Map<Long, User> userMap = userMapper.selectBatchIds(authorIds).stream()
                    .collect(Collectors.toMap(User::getUserId, u -> u));
            for (GroupPost p : posts) {
                User u = userMap.get(p.getAuthorId());
                if (u != null) {
                    Map<String, Object> brief = new HashMap<>();
                    brief.put("userId", u.getUserId());
                    brief.put("nickname", u.getNickname());
                    brief.put("avatarUrl", u.getAvatarUrl());
                    p.setAuthor(brief);
                }
                p.setIsMine(p.getAuthorId().equals(currentUserId));
                p.setIsLiked(false);
            }
        }
        return posts;
    }

    @Override
    public GroupPost createGroupPost(Long groupId, DynamicCreateRequest req) {
        Long currentUserId = securityUtil.getCurrentUserId();
        QueryWrapper<GroupMember> memberWrapper = new QueryWrapper<>();
        memberWrapper.eq("group_id", groupId).eq("user_id", currentUserId);
        if (groupMemberMapper.selectCount(memberWrapper) == 0) {
            throw new RuntimeException("不是圈子成员，无法发帖");
        }
        GroupPost post = new GroupPost();
        post.setGroupId(groupId);
        post.setAuthorId(currentUserId);
        post.setContent(req.getContent());
        post.setLikeCount(0);
        post.setPublishTime(LocalDateTime.now());
        groupPostMapper.insert(post);

        User author = userMapper.selectById(currentUserId);
        if (author != null) {
            Map<String, Object> brief = new HashMap<>();
            brief.put("userId", author.getUserId());
            brief.put("nickname", author.getNickname());
            brief.put("avatarUrl", author.getAvatarUrl());
            post.setAuthor(brief);
        }
        post.setIsMine(true);
        post.setIsLiked(false);
        return post;
    }

    @Override
    public void deleteGroupPost(Long groupPostId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        GroupPost post = groupPostMapper.selectById(groupPostId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        if (!post.getAuthorId().equals(currentUserId)) {
            throw new RuntimeException("只能删除自己的帖子");
        }
        groupPostMapper.deleteById(groupPostId);
    }
}
