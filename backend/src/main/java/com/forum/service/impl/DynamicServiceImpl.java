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
import com.forum.service.DynamicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DynamicServiceImpl implements DynamicService {

    private final RealtimeDynamicMapper realtimeDynamicMapper;
    private final FollowMapper followMapper;
    private final SecurityUtil securityUtil;

    @Override
    public PageResult<RealtimeDynamic> getFeed(int page, int pageSize, String filter) {
        Page<RealtimeDynamic> mpPage = new Page<>(page, pageSize);
        QueryWrapper<RealtimeDynamic> wrapper = new QueryWrapper<>();

        if ("following".equals(filter)) {
            Long currentUserId = securityUtil.getCurrentUserId();
            List<Follow> followings = followMapper.selectByFollowerId(currentUserId);
            if (followings.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0, page, pageSize);
            }
            List<Long> followeeIds = followings.stream()
                    .map(Follow::getFolloweeId)
                    .collect(Collectors.toList());
            wrapper.in("author_id", followeeIds)
                   .orderByDesc("publish_time");
        } else if ("hot".equals(filter)) {
            wrapper.orderByDesc("like_count");
        } else {
            wrapper.orderByDesc("publish_time");
        }

        Page<RealtimeDynamic> result = realtimeDynamicMapper.selectPage(mpPage, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    public RealtimeDynamic createDynamic(DynamicCreateRequest req) {
        Long currentUserId = securityUtil.getCurrentUserId();
        RealtimeDynamic dynamic = new RealtimeDynamic();
        dynamic.setAuthorId(currentUserId);
        dynamic.setContent(req.getContent());
        dynamic.setLikeCount(0);
        dynamic.setPublishTime(LocalDateTime.now());
        realtimeDynamicMapper.insert(dynamic);
        return dynamic;
    }

    @Override
    public void deleteDynamic(Long dynamicId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        RealtimeDynamic dynamic = realtimeDynamicMapper.selectById(dynamicId);
        if (dynamic == null) {
            throw new RuntimeException("动态不存在");
        }
        if (!dynamic.getAuthorId().equals(currentUserId)) {
            throw new RuntimeException("只能删除自己的动态");
        }
        realtimeDynamicMapper.deleteById(dynamicId);
    }

    @Override
    public List<RealtimeDynamic> getByUser(Long userId) {
        QueryWrapper<RealtimeDynamic> wrapper = new QueryWrapper<>();
        wrapper.eq("author_id", userId).orderByDesc("publish_time");
        return realtimeDynamicMapper.selectList(wrapper);
    }
}
