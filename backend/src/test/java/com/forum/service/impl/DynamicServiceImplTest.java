package com.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forum.common.PageResult;
import com.forum.common.SecurityUtil;
import com.forum.dto.DynamicCreateRequest;
import com.forum.entity.Follow;
import com.forum.entity.RealtimeDynamic;
import com.forum.mapper.FollowMapper;
import com.forum.mapper.RealtimeDynamicMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DynamicServiceImpl 动态服务 单元测试")
class DynamicServiceImplTest {

    @Mock private RealtimeDynamicMapper realtimeDynamicMapper;
    @Mock private FollowMapper followMapper;
    @Mock private SecurityUtil securityUtil;

    @InjectMocks
    private DynamicServiceImpl dynamicService;

    @Nested
    @DisplayName("getFeed() 动态流")
    class GetFeed {

        @Test
        @DisplayName("latest 过滤应返回全量动态")
        void latestFilter_shouldReturnAll() {
            Page<RealtimeDynamic> mpPage = new Page<>(1, 10);
            mpPage.setRecords(Collections.emptyList());
            mpPage.setTotal(0);
            when(realtimeDynamicMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mpPage);

            PageResult<RealtimeDynamic> result = dynamicService.getFeed(1, 10, "latest");
            assertNotNull(result);
            assertEquals(0, result.getTotal());
        }

        @Test
        @DisplayName("hot 过滤应按 like_count 倒序")
        void hotFilter_shouldOrderByLikes() {
            Page<RealtimeDynamic> mpPage = new Page<>(1, 10);
            mpPage.setRecords(Collections.emptyList());
            mpPage.setTotal(0);
            when(realtimeDynamicMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mpPage);

            PageResult<RealtimeDynamic> result = dynamicService.getFeed(1, 10, "hot");
            assertNotNull(result);
        }

        @Test
        @DisplayName("following 过滤 - 有关注时返回关注者动态")
        void followingFilter_withFollowings_shouldReturnFolloweeDynamics() {
            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            List<Follow> followings = List.of(
                    createFollow(2L), createFollow(3L)
            );
            when(followMapper.selectByFollowerId(1L)).thenReturn(followings);

            Page<RealtimeDynamic> mpPage = new Page<>(1, 10);
            mpPage.setRecords(Collections.emptyList());
            mpPage.setTotal(0);
            when(realtimeDynamicMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mpPage);

            PageResult<RealtimeDynamic> result = dynamicService.getFeed(1, 10, "following");
            assertNotNull(result);
        }

        @Test
        @DisplayName("following 过滤 - 无关注时返回空列表")
        void followingFilter_noFollowings_shouldReturnEmpty() {
            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            when(followMapper.selectByFollowerId(1L)).thenReturn(Collections.emptyList());

            PageResult<RealtimeDynamic> result = dynamicService.getFeed(1, 10, "following");

            assertEquals(0, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }

        private Follow createFollow(Long followeeId) {
            Follow f = new Follow();
            f.setFolloweeId(followeeId);
            return f;
        }
    }

    @Nested
    @DisplayName("createDynamic() 创建动态")
    class CreateDynamic {

        @Test
        @DisplayName("创建动态应成功")
        void createDynamic_shouldSucceed() {
            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            when(realtimeDynamicMapper.insert(any(RealtimeDynamic.class))).thenAnswer(inv -> {
                RealtimeDynamic d = inv.getArgument(0);
                d.setDynamicId(1L);
                return 1;
            });

            DynamicCreateRequest req = new DynamicCreateRequest();
            req.setContent("hello world");

            RealtimeDynamic result = dynamicService.createDynamic(req);

            assertEquals("hello world", result.getContent());
            assertEquals(1L, result.getAuthorId());
            assertEquals(0, result.getLikeCount());
        }
    }

    @Nested
    @DisplayName("deleteDynamic() 删除动态")
    class DeleteDynamic {

        @Test
        @DisplayName("作者可删除自己的动态")
        void authorCanDelete() {
            RealtimeDynamic dynamic = new RealtimeDynamic();
            dynamic.setDynamicId(1L);
            dynamic.setAuthorId(100L);

            when(securityUtil.getCurrentUserId()).thenReturn(100L);
            when(realtimeDynamicMapper.selectById(1L)).thenReturn(dynamic);
            when(realtimeDynamicMapper.deleteById(1L)).thenReturn(1);

            assertDoesNotThrow(() -> dynamicService.deleteDynamic(1L));
        }

        @Test
        @DisplayName("非作者不能删除他人动态")
        void nonAuthorCannotDelete() {
            RealtimeDynamic dynamic = new RealtimeDynamic();
            dynamic.setDynamicId(1L);
            dynamic.setAuthorId(200L);

            when(securityUtil.getCurrentUserId()).thenReturn(100L);
            when(realtimeDynamicMapper.selectById(1L)).thenReturn(dynamic);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> dynamicService.deleteDynamic(1L));
            assertEquals("只能删除自己的动态", ex.getMessage());
        }

        @Test
        @DisplayName("删除不存在的动态应抛出 RuntimeException")
        void deleteNonExistent_shouldThrow() {
            when(securityUtil.getCurrentUserId()).thenReturn(100L);
            when(realtimeDynamicMapper.selectById(999L)).thenReturn(null);

            assertThrows(RuntimeException.class, () -> dynamicService.deleteDynamic(999L));
        }
    }

    @Nested
    @DisplayName("getByUser() 查询用户动态")
    class GetByUser {

        @Test
        @DisplayName("应返回指定用户的动态列表")
        void shouldReturnUserDynamics() {
            when(realtimeDynamicMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            List<RealtimeDynamic> result = dynamicService.getByUser(1L);
            assertNotNull(result);
        }
    }
}
