package com.forum.service;

import com.forum.common.PageResult;
import com.forum.entity.Notification;

public interface NotificationService {

    PageResult<Notification> getNotifications(int page, int pageSize);

    int getUnreadCount();

    void markRead(Long notificationId);

    void markAllRead();
}
