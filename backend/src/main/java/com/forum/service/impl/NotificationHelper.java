package com.forum.service.impl;

import com.forum.entity.Notification;
import com.forum.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class NotificationHelper {

    private final NotificationMapper notificationMapper;

    public void createNotification(Long userId, int notifyType, String title, String content,
                                   int targetType, Long targetId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setNotifyType(notifyType);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }
}
