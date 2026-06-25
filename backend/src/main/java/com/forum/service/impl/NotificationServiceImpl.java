package com.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forum.common.PageResult;
import com.forum.common.SecurityUtil;
import com.forum.entity.Notification;
import com.forum.mapper.NotificationMapper;
import com.forum.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final SecurityUtil securityUtil;

    @Override
    public PageResult<Notification> getNotifications(int page, int pageSize) {
        Long currentUserId = securityUtil.getCurrentUserId();
        Page<Notification> mpPage = new Page<>(page, pageSize);
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", currentUserId).orderByDesc("created_at");
        Page<Notification> result = notificationMapper.selectPage(mpPage, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    public int getUnreadCount() {
        Long currentUserId = securityUtil.getCurrentUserId();
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", currentUserId).eq("is_read", 0);
        return notificationMapper.selectCount(wrapper).intValue();
    }

    @Override
    public void markRead(Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new RuntimeException("通知不存在");
        }
        notification.setIsRead(1);
        notificationMapper.updateById(notification);
    }

    @Override
    public void markAllRead() {
        Long currentUserId = securityUtil.getCurrentUserId();
        Notification update = new Notification();
        update.setIsRead(1);
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", currentUserId).eq("is_read", 0);
        notificationMapper.update(update, wrapper);
    }
}
