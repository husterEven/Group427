package com.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.forum.common.SecurityUtil;
import com.forum.dto.VoteCreateRequest;
import com.forum.dto.VoteSubmitRequest;
import com.forum.entity.VotePost;
import com.forum.entity.VoteRecord;
import com.forum.mapper.VotePostMapper;
import com.forum.mapper.VoteRecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoteServiceImpl 投票服务 单元测试")
class VoteServiceImplTest {

    @Mock private VotePostMapper votePostMapper;
    @Mock private VoteRecordMapper voteRecordMapper;
    @Mock private SecurityUtil securityUtil;

    @InjectMocks
    private VoteServiceImpl voteService;

    @Nested
    @DisplayName("createVote() 创建投票")
    class CreateVote {

        @Test
        @DisplayName("有截止日期时应解析成功")
        void withEndTime_shouldParseSuccessfully() {
            when(votePostMapper.insert(any(VotePost.class))).thenAnswer(inv -> {
                VotePost v = inv.getArgument(0);
                v.setVoteId(1L);
                return 1;
            });

            VoteCreateRequest req = new VoteCreateRequest();
            req.setVoteTitle("test vote");
            req.setEndTime("2027-12-31T23:59:59");

            VotePost result = voteService.createVote(1L, req);

            assertEquals("test vote", result.getVoteTitle());
            assertNotNull(result.getEndTime());
        }

        @Test
        @DisplayName("无截止日期时 endTime 为 null")
        void withoutEndTime_shouldBeNull() {
            when(votePostMapper.insert(any(VotePost.class))).thenAnswer(inv -> {
                VotePost v = inv.getArgument(0);
                v.setVoteId(1L);
                return 1;
            });

            VoteCreateRequest req = new VoteCreateRequest();
            req.setVoteTitle("test vote");
            req.setEndTime(null);

            VotePost result = voteService.createVote(1L, req);
            assertNull(result.getEndTime());
        }

        @Test
        @DisplayName("空字符串截止日期不解析")
        void emptyEndTime_shouldNotParse() {
            when(votePostMapper.insert(any(VotePost.class))).thenAnswer(inv -> {
                VotePost v = inv.getArgument(0);
                v.setVoteId(1L);
                return 1;
            });

            VoteCreateRequest req = new VoteCreateRequest();
            req.setVoteTitle("test vote");
            req.setEndTime("");

            VotePost result = voteService.createVote(1L, req);
            assertNull(result.getEndTime());
        }
    }

    @Nested
    @DisplayName("submitVote() 提交投票")
    class SubmitVote {

        @Test
        @DisplayName("首次投票应成功")
        void firstVote_shouldSucceed() {
            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            when(voteRecordMapper.selectByVoteAndUser(1L, 1L)).thenReturn(null);
            when(voteRecordMapper.insert(any(VoteRecord.class))).thenReturn(1);

            VoteSubmitRequest req = new VoteSubmitRequest();
            req.setOptionIndex(0);

            Map<String, Object> result = voteService.submitVote(1L, req);

            assertEquals(true, result.get("submitted"));
        }

        @Test
        @DisplayName("重复投票应抛出 RuntimeException")
        void duplicateVote_shouldThrow() {
            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            when(voteRecordMapper.selectByVoteAndUser(1L, 1L)).thenReturn(new VoteRecord());

            VoteSubmitRequest req = new VoteSubmitRequest();
            req.setOptionIndex(1);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> voteService.submitVote(1L, req));
            assertEquals("您已经投过票了", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("getVoteByPost() 查看投票")
    class GetVoteByPost {

        @Test
        @DisplayName("投票不存在应抛出 RuntimeException")
        void nonExistentVote_shouldThrow() {
            when(votePostMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> voteService.getVoteByPost(1L));
            assertEquals("投票不存在", ex.getMessage());
        }

        @Test
        @DisplayName("未截止投票应返回 isExpired=false")
        void nonExpiredVote_shouldReturnNotExpired() {
            VotePost vote = new VotePost();
            vote.setVoteId(1L);
            vote.setVoteTitle("poll");
            vote.setEndTime(LocalDateTime.now().plusDays(7));

            when(votePostMapper.selectOne(any(QueryWrapper.class))).thenReturn(vote);
            when(voteRecordMapper.countByVoteId(1L)).thenReturn(java.util.Collections.emptyList());
            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            when(voteRecordMapper.selectByVoteAndUser(1L, 1L)).thenReturn(null);

            Map<String, Object> result = voteService.getVoteByPost(1L);

            assertEquals(false, result.get("isExpired"));
            assertEquals(1L, result.get("voteId"));
            assertNull(result.get("mySelection"));
        }

        @Test
        @DisplayName("已过期投票应返回 isExpired=true")
        void expiredVote_shouldReturnExpired() {
            VotePost vote = new VotePost();
            vote.setVoteId(1L);
            vote.setVoteTitle("expired poll");
            vote.setEndTime(LocalDateTime.now().minusDays(1));

            when(votePostMapper.selectOne(any(QueryWrapper.class))).thenReturn(vote);
            when(voteRecordMapper.countByVoteId(1L)).thenReturn(java.util.Collections.emptyList());
            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            when(voteRecordMapper.selectByVoteAndUser(1L, 1L)).thenReturn(null);

            Map<String, Object> result = voteService.getVoteByPost(1L);

            assertEquals(true, result.get("isExpired"));
        }

        @Test
        @DisplayName("用户已投票应返回 mySelection")
        void votedByUser_shouldReturnSelection() {
            VotePost vote = new VotePost();
            vote.setVoteId(1L);
            vote.setVoteTitle("poll");
            vote.setEndTime(null);

            when(votePostMapper.selectOne(any(QueryWrapper.class))).thenReturn(vote);
            when(voteRecordMapper.countByVoteId(1L)).thenReturn(java.util.Collections.emptyList());
            when(securityUtil.getCurrentUserId()).thenReturn(1L);

            VoteRecord myVote = new VoteRecord();
            myVote.setOptionIndex(1);
            when(voteRecordMapper.selectByVoteAndUser(1L, 1L)).thenReturn(myVote);

            Map<String, Object> result = voteService.getVoteByPost(1L);
            assertEquals(1, result.get("mySelection"));
        }
    }
}
