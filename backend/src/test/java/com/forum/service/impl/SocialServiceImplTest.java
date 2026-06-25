package com.forum.service.impl;

import com.forum.common.SecurityUtil;
import com.forum.dto.*;
import com.forum.entity.*;
import com.forum.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocialServiceImpl 社交服务 控制结构单元测试")
class SocialServiceImplTest {

    @Mock private FollowMapper followMapper;
    @Mock private PrivateMessageMapper privateMessageMapper;
    @Mock private GroupInfoMapper groupInfoMapper;
    @Mock private GroupMemberMapper groupMemberMapper;
    @Mock private GroupPostMapper groupPostMapper;
    @Mock private UserMapper userMapper;
    @Mock private SecurityUtil securityUtil;
    @Mock private NotificationHelper notificationHelper;

    @InjectMocks
    private SocialServiceImpl socialService;

    @BeforeEach
    void setUp() {
        lenient().when(securityUtil.getCurrentUserId()).thenReturn(1L);
    }

    @Nested
    @DisplayName("toggleFollow() - 关注/取关")
    class ToggleFollow {

        @Test
        @DisplayName("关注自己应抛出异常 (if: currentUserId.equals(followeeId)=true)")
        void followSelf_shouldThrowException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.toggleFollow(1L));
            assertEquals("不能关注自己", ex.getMessage());
        }

        @Test
        @DisplayName("已关注则取消关注 (if: existing!=null=true -> delete)")
        void alreadyFollowing_shouldUnfollow() {
            Follow existing = new Follow();
            existing.setRelationId(10L);
            when(followMapper.selectByBoth(1L, 2L)).thenReturn(existing);

            socialService.toggleFollow(2L);

            verify(followMapper).deleteById(10L);
            verify(followMapper, never()).insert(any());
        }

        @Test
        @DisplayName("未关注则关注 (if: existing!=null=false -> insert)")
        void notFollowing_shouldFollow() {
            when(followMapper.selectByBoth(1L, 2L)).thenReturn(null);

            socialService.toggleFollow(2L);

            ArgumentCaptor<Follow> captor = ArgumentCaptor.forClass(Follow.class);
            verify(followMapper).insert(captor.capture());
            Follow saved = captor.getValue();
            assertEquals(1L, saved.getFollowerId());
            assertEquals(2L, saved.getFolloweeId());
            assertEquals(0, saved.getIsStarred());
            assertNotNull(saved.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("setStar() - 星标设置")
    class SetStar {

        @Test
        @DisplayName("未关注用户设置星标应抛出异常 (if: follow==null=true)")
        void notFollowing_shouldThrowException() {
            when(followMapper.selectByBoth(1L, 2L)).thenReturn(null);

            StarRequest req = new StarRequest();
            req.setIsStarred(true);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.setStar(2L, req));
            assertEquals("未关注该用户", ex.getMessage());
        }

        @Test
        @DisplayName("设置星标为true (三元: isStarred?1:0 -> 1)")
        void setStarTrue_shouldSetToOne() {
            Follow follow = new Follow();
            follow.setRelationId(1L);
            when(followMapper.selectByBoth(1L, 2L)).thenReturn(follow);

            StarRequest req = new StarRequest();
            req.setIsStarred(true);

            socialService.setStar(2L, req);

            ArgumentCaptor<Follow> captor = ArgumentCaptor.forClass(Follow.class);
            verify(followMapper).updateById(captor.capture());
            assertEquals(1, captor.getValue().getIsStarred());
        }

        @Test
        @DisplayName("取消星标 (三元: isStarred?1:0 -> 0)")
        void setStarFalse_shouldSetToZero() {
            Follow follow = new Follow();
            follow.setRelationId(1L);
            when(followMapper.selectByBoth(1L, 2L)).thenReturn(follow);

            StarRequest req = new StarRequest();
            req.setIsStarred(false);

            socialService.setStar(2L, req);

            ArgumentCaptor<Follow> captor = ArgumentCaptor.forClass(Follow.class);
            verify(followMapper).updateById(captor.capture());
            assertEquals(0, captor.getValue().getIsStarred());
        }
    }

    @Nested
    @DisplayName("sendMessage() - 发送私信")
    class SendMessage {

        @Test
        @DisplayName("向自己发消息应抛出异常 (if: currentUserId.equals(targetUserId)=true)")
        void sendToSelf_shouldThrowException() {
            MessageSendRequest req = new MessageSendRequest();
            req.setContent("hello");

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.sendMessage(1L, req));
            assertEquals("不能给自己发消息", ex.getMessage());
        }

        @Test
        @DisplayName("向他人发消息应成功插入")
        void sendToOther_shouldInsert() {
            MessageSendRequest req = new MessageSendRequest();
            req.setContent("你好");

            PrivateMessage result = socialService.sendMessage(2L, req);

            ArgumentCaptor<PrivateMessage> captor = ArgumentCaptor.forClass(PrivateMessage.class);
            verify(privateMessageMapper).insert(captor.capture());
            PrivateMessage saved = captor.getValue();
            assertEquals(1L, saved.getSenderId());
            assertEquals(2L, saved.getReceiverId());
            assertEquals("你好", saved.getContent());
            assertEquals(0, saved.getIsRead());
            assertNotNull(saved.getSendTime());
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("createGroup() - 创建圈子")
    class CreateGroup {

        @Test
        @DisplayName("创建圈子应设置mode默认值 (三元: req.getMode()!=null?mode:0 -> 0)")
        void createWithNullMode_shouldDefaultToZero() {
            GroupCreateRequest req = new GroupCreateRequest();
            req.setGroupName("投资交流圈");

            socialService.createGroup(req);

            ArgumentCaptor<GroupInfo> captor = ArgumentCaptor.forClass(GroupInfo.class);
            verify(groupInfoMapper).insert(captor.capture());
            assertEquals("投资交流圈", captor.getValue().getGroupName());
            assertEquals(0, captor.getValue().getMode());
            assertEquals(0, captor.getValue().getStatus());
            assertEquals(1L, captor.getValue().getOwnerId());
        }

        @Test
        @DisplayName("创建圈子mode非null应使用传入值 (三元: req.getMode()!=null?mode:0 -> mode)")
        void createWithExplicitMode_shouldUseProvidedValue() {
            GroupCreateRequest req = new GroupCreateRequest();
            req.setGroupName("私密圈");
            req.setMode(1);

            socialService.createGroup(req);

            ArgumentCaptor<GroupInfo> captor = ArgumentCaptor.forClass(GroupInfo.class);
            verify(groupInfoMapper).insert(captor.capture());
            assertEquals(1, captor.getValue().getMode());
        }

        @Test
        @DisplayName("创建者应自动成为管理员 (role=2)")
        void creator_shouldBeAddedAsAdmin() {
            GroupCreateRequest req = new GroupCreateRequest();
            req.setGroupName("test group");

            socialService.createGroup(req);

            ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
            verify(groupMemberMapper).insert(captor.capture());
            assertEquals(1L, captor.getValue().getUserId());
            assertEquals(2, captor.getValue().getRole());
            assertNotNull(captor.getValue().getJoinedAt());
        }
    }

    @Nested
    @DisplayName("joinGroup() - 加入圈子")
    class JoinGroup {

        private GroupInfo buildGroup(int mode) {
            GroupInfo group = new GroupInfo();
            group.setGroupId(100L);
            group.setGroupName("test");
            group.setOwnerId(99L);
            group.setMode(mode);
            group.setStatus(0);
            return group;
        }

        @Test
        @DisplayName("圈子不存在应抛出异常 (if: group==null=true)")
        void nonExistentGroup_shouldThrow() {
            when(groupInfoMapper.selectById(999L)).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.joinGroup(999L));
            assertEquals("圈子不存在", ex.getMessage());
        }

        @Test
        @DisplayName("已是成员应抛出异常 (if: count>0=true)")
        void alreadyMember_shouldThrow() {
            GroupInfo group = buildGroup(0);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);
            when(groupMemberMapper.selectCount(any())).thenReturn(1L);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.joinGroup(100L));
            assertEquals("已经是圈子成员", ex.getMessage());
        }

        @Test
        @DisplayName("mode=1需审核应抛出异常 (if: mode==1=true)")
        void modeNeedsReview_shouldThrow() {
            GroupInfo group = buildGroup(1);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);
            when(groupMemberMapper.selectCount(any())).thenReturn(0L);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.joinGroup(100L));
            assertEquals("该圈子需要审核才能加入", ex.getMessage());
        }

        @Test
        @DisplayName("mode=2禁止加入应抛出异常 (if: mode==2=true)")
        void modeForbidden_shouldThrow() {
            GroupInfo group = buildGroup(2);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);
            when(groupMemberMapper.selectCount(any())).thenReturn(0L);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.joinGroup(100L));
            assertEquals("该圈子禁止加入", ex.getMessage());
        }

        @Test
        @DisplayName("mode=0公开圈子应成功加入 (三个if全部false)")
        void publicGroup_shouldJoinSuccessfully() {
            GroupInfo group = buildGroup(0);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);
            when(groupMemberMapper.selectCount(any())).thenReturn(0L);

            socialService.joinGroup(100L);

            ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
            verify(groupMemberMapper).insert(captor.capture());
            assertEquals(100L, captor.getValue().getGroupId());
            assertEquals(1L, captor.getValue().getUserId());
            assertEquals(0, captor.getValue().getRole());
        }
    }

    @Nested
    @DisplayName("leaveGroup() - 退出圈子")
    class LeaveGroup {

        @Test
        @DisplayName("非成员退出应抛出异常 (if: member==null=true)")
        void notMember_shouldThrow() {
            GroupInfo group = new GroupInfo();
            group.setGroupId(100L);
            group.setOwnerId(99L);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);
            when(groupMemberMapper.selectOne(any())).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.leaveGroup(100L));
            assertEquals("不是圈子成员", ex.getMessage());
        }

        @Test
        @DisplayName("普通成员退出不触发所有权转移 (if: ownerId.equals=false)")
        void normalMemberLeave_shouldNotTransferOwnership() {
            GroupInfo group = new GroupInfo();
            group.setGroupId(100L);
            group.setOwnerId(99L);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);

            GroupMember member = new GroupMember();
            member.setMemberId(5L);
            when(groupMemberMapper.selectOne(any())).thenReturn(member);

            socialService.leaveGroup(100L);

            verify(groupMemberMapper).deleteById(5L);
            verify(groupMemberMapper, never()).selectList(any());
            verify(groupInfoMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("圈主退出且有剩余成员应转移所有权 (if: ownerId.equals=true && !remaining.isEmpty())")
        void ownerLeaveWithMembers_shouldTransferOwnership() {
            GroupInfo group = new GroupInfo();
            group.setGroupId(100L);
            group.setOwnerId(1L);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);

            GroupMember member = new GroupMember();
            member.setMemberId(1L);
            when(groupMemberMapper.selectOne(any())).thenReturn(member);

            GroupMember nextMember = new GroupMember();
            nextMember.setMemberId(2L);
            nextMember.setUserId(5L);
            when(groupMemberMapper.selectList(any())).thenReturn(List.of(nextMember));

            socialService.leaveGroup(100L);

            verify(groupMemberMapper).deleteById(1L);
            ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
            verify(groupMemberMapper).updateById(captor.capture());
            assertEquals(2, captor.getValue().getRole());

            verify(groupInfoMapper).updateById(any(GroupInfo.class));
            verify(groupInfoMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("圈主退出且无剩余成员应删除圈子 (if: ownerId.equals=true && remaining.isEmpty())")
        void ownerLeaveNoMembers_shouldDeleteGroup() {
            GroupInfo group = new GroupInfo();
            group.setGroupId(100L);
            group.setOwnerId(1L);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);

            GroupMember member = new GroupMember();
            member.setMemberId(1L);
            when(groupMemberMapper.selectOne(any())).thenReturn(member);
            when(groupMemberMapper.selectList(any())).thenReturn(List.of());

            socialService.leaveGroup(100L);

            verify(groupMemberMapper).deleteById(1L);
            verify(groupInfoMapper).deleteById(100L);
        }
    }

    @Nested
    @DisplayName("updateGroup() - 修改圈子信息")
    class UpdateGroup {

        @Test
        @DisplayName("非圈主修改应抛出异常 (if: !ownerId.equals=true)")
        void nonOwnerUpdate_shouldThrow() {
            GroupInfo group = new GroupInfo();
            group.setGroupId(100L);
            group.setOwnerId(99L);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);

            GroupUpdateRequest req = new GroupUpdateRequest();
            req.setGroupName("新名字");

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.updateGroup(100L, req));
            assertEquals("只有圈主才能修改圈子信息", ex.getMessage());
        }

        @Test
        @DisplayName("圈主部分更新应仅更新非null字段 (if: req.getXxx()!=null)")
        void ownerPartialUpdate_shouldOnlyUpdateNonNull() {
            GroupInfo group = new GroupInfo();
            group.setGroupId(100L);
            group.setOwnerId(1L);
            group.setGroupName("旧名");
            group.setMode(0);
            group.setStatus(0);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);

            GroupUpdateRequest req = new GroupUpdateRequest();
            req.setGroupName("新名");
            // mode and status are null

            socialService.updateGroup(100L, req);

            ArgumentCaptor<GroupInfo> captor = ArgumentCaptor.forClass(GroupInfo.class);
            verify(groupInfoMapper).updateById(captor.capture());
            assertEquals("新名", captor.getValue().getGroupName());
            assertEquals(0, captor.getValue().getMode());    // unchanged
        }

        @Test
        @DisplayName("圈主全量更新所有字段")
        void ownerFullUpdate_shouldUpdateAllFields() {
            GroupInfo group = new GroupInfo();
            group.setGroupId(100L);
            group.setOwnerId(1L);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);

            GroupUpdateRequest req = new GroupUpdateRequest();
            req.setGroupName("新名");
            req.setMode(1);
            req.setStatus(1);

            socialService.updateGroup(100L, req);

            ArgumentCaptor<GroupInfo> captor = ArgumentCaptor.forClass(GroupInfo.class);
            verify(groupInfoMapper).updateById(captor.capture());
            assertEquals("新名", captor.getValue().getGroupName());
            assertEquals(1, captor.getValue().getMode());
            assertEquals(1, captor.getValue().getStatus());
        }
    }

    @Nested
    @DisplayName("setRole() - 设置成员角色")
    class SetRole {

        @Test
        @DisplayName("无权限设置角色 (if: currentMember==null || role<1=true) - 非成员")
        void notGroupMember_shouldThrow() {
            GroupInfo group = new GroupInfo();
            group.setGroupId(100L);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);
            when(groupMemberMapper.selectOne(any())).thenReturn(null); // not a member

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.setRole(100L, 5L, 1));
            assertEquals("无权设置角色", ex.getMessage());
        }

        @Test
        @DisplayName("普通成员设置角色 (if: role<1=true)")
        void normalMemberSetRole_shouldThrow() {
            GroupInfo group = new GroupInfo();
            group.setGroupId(100L);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);

            GroupMember currentMember = new GroupMember();
            currentMember.setRole(0);
            when(groupMemberMapper.selectOne(any())).thenReturn(currentMember);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.setRole(100L, 5L, 1));
            assertEquals("无权设置角色", ex.getMessage());
        }

        @Test
        @DisplayName("管理员设置角色应成功")
        void adminSetRole_shouldSucceed() {
            GroupInfo group = new GroupInfo();
            group.setGroupId(100L);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);

            GroupMember currentMember = new GroupMember();
            currentMember.setRole(1);
            when(groupMemberMapper.selectOne(any())).thenReturn(currentMember);

            GroupMember targetMember = new GroupMember();
            targetMember.setMemberId(10L);
            targetMember.setRole(0);
            when(groupMemberMapper.selectOne(any())).thenReturn(currentMember, targetMember);

            socialService.setRole(100L, 5L, 1);

            ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
            verify(groupMemberMapper).updateById(captor.capture());
            assertEquals(1, captor.getValue().getRole());
        }
    }

    @Nested
    @DisplayName("kickMember() - 踢出成员")
    class KickMember {

        @Test
        @DisplayName("非圈主踢人应抛出异常 (if: !ownerId.equals=true)")
        void nonOwnerKick_shouldThrow() {
            GroupInfo group = new GroupInfo();
            group.setGroupId(100L);
            group.setOwnerId(99L);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.kickMember(100L, 5L));
            assertEquals("只有圈主才能踢人", ex.getMessage());
        }

        @Test
        @DisplayName("踢圈主自身应抛出异常 (if: memberUserId.equals(ownerId)=true)")
        void kickOwner_shouldThrow() {
            GroupInfo group = new GroupInfo();
            group.setGroupId(100L);
            group.setOwnerId(1L);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.kickMember(100L, 1L));
            assertEquals("不能踢出圈主", ex.getMessage());
        }

        @Test
        @DisplayName("目标成员不在圈内应抛出异常 (if: targetMember==null=true)")
        void targetNotMember_shouldThrow() {
            GroupInfo group = new GroupInfo();
            group.setGroupId(100L);
            group.setOwnerId(1L);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);
            when(groupMemberMapper.selectOne(any())).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.kickMember(100L, 5L));
            assertEquals("目标成员不在圈内", ex.getMessage());
        }

        @Test
        @DisplayName("圈主踢普通成员应成功 (所有if=false)")
        void ownerKickMember_shouldSucceed() {
            GroupInfo group = new GroupInfo();
            group.setGroupId(100L);
            group.setOwnerId(1L);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);

            GroupMember target = new GroupMember();
            target.setMemberId(10L);
            when(groupMemberMapper.selectOne(any())).thenReturn(target);

            socialService.kickMember(100L, 5L);

            verify(groupMemberMapper).deleteById(10L);
        }
    }

    @Nested
    @DisplayName("createGroupPost() - 圈子发帖")
    class CreateGroupPost {

        @Test
        @DisplayName("非成员发帖应抛出异常 (if: count==0=true)")
        void nonMemberPost_shouldThrow() {
            when(groupMemberMapper.selectCount(any())).thenReturn(0L);

            DynamicCreateRequest req = new DynamicCreateRequest();
            req.setContent("test content");

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.createGroupPost(100L, req));
            assertEquals("不是圈子成员，无法发帖", ex.getMessage());
        }

        @Test
        @DisplayName("成员发帖应成功")
        void memberPost_shouldSucceed() {
            when(groupMemberMapper.selectCount(any())).thenReturn(1L);

            DynamicCreateRequest req = new DynamicCreateRequest();
            req.setContent("圈子帖子内容");

            GroupPost result = socialService.createGroupPost(100L, req);

            ArgumentCaptor<GroupPost> captor = ArgumentCaptor.forClass(GroupPost.class);
            verify(groupPostMapper).insert(captor.capture());
            assertEquals(100L, captor.getValue().getGroupId());
            assertEquals(1L, captor.getValue().getAuthorId());
            assertEquals("圈子帖子内容", captor.getValue().getContent());
            assertEquals(0, captor.getValue().getLikeCount());
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("deleteGroupPost() - 删除圈子帖子")
    class DeleteGroupPost {

        @Test
        @DisplayName("帖子不存在应抛出异常 (if: post==null=true)")
        void nonExistentPost_shouldThrow() {
            when(groupPostMapper.selectById(999L)).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.deleteGroupPost(999L));
            assertEquals("帖子不存在", ex.getMessage());
        }

        @Test
        @DisplayName("非作者删除应抛出异常 (if: !authorId.equals=true)")
        void nonAuthorDelete_shouldThrow() {
            GroupPost post = new GroupPost();
            post.setGroupPostId(10L);
            post.setAuthorId(99L);
            when(groupPostMapper.selectById(10L)).thenReturn(post);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.deleteGroupPost(10L));
            assertEquals("只能删除自己的帖子", ex.getMessage());
        }

        @Test
        @DisplayName("作者删除应成功")
        void authorDelete_shouldSucceed() {
            GroupPost post = new GroupPost();
            post.setGroupPostId(10L);
            post.setAuthorId(1L);
            when(groupPostMapper.selectById(10L)).thenReturn(post);

            socialService.deleteGroupPost(10L);

            verify(groupPostMapper).deleteById(10L);
        }
    }

    @Nested
    @DisplayName("enrichFollows() - 关注列表增强 (私有方法，通过getFollowing/getFollowers间接测试)")
    class EnrichFollows {

        @Test
        @DisplayName("空关注列表应正常返回 (if: follows.isEmpty()=true -> return)")
        void emptyFollows_shouldReturnEmptyList() {
            when(followMapper.selectByFollowerId(2L)).thenReturn(List.of());

            List<Follow> result = socialService.getFollowing(2L);

            assertTrue(result.isEmpty());
            verify(userMapper, never()).selectBatchIds(anySet());
        }
    }

    @Nested
    @DisplayName("getGroupDetail() - 圈子详情")
    class GetGroupDetail {

        @Test
        @DisplayName("非成员查看应抛出异常 (if: count==0=true)")
        void nonMemberView_shouldThrow() {
            GroupInfo group = new GroupInfo();
            group.setGroupId(100L);
            when(groupInfoMapper.selectById(100L)).thenReturn(group);
            when(groupMemberMapper.selectCount(any())).thenReturn(0L);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> socialService.getGroupDetail(100L));
            assertEquals("你不是该圈子的成员", ex.getMessage());
        }

        @Test
        @DisplayName("成员查看应返回圈子信息")
        void memberView_shouldReturnGroup() {
            GroupInfo group = new GroupInfo();
            group.setGroupId(100L);
            group.setGroupName("测试圈");
            when(groupInfoMapper.selectById(100L)).thenReturn(group);
            when(groupMemberMapper.selectCount(any())).thenReturn(1L);

            GroupInfo result = socialService.getGroupDetail(100L);

            assertEquals("测试圈", result.getGroupName());
        }
    }
}
