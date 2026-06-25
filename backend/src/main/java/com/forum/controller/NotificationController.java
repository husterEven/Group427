package com.forum.controller;

import com.forum.common.Result;
import com.forum.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Result<?> getNotifications(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(notificationService.getNotifications(page, pageSize));
    }

    @GetMapping("/unread-count")
    public Result<?> getUnreadCount() {
        return Result.ok(notificationService.getUnreadCount());
    }

    @PutMapping("/{id}/read")
    public Result<?> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.ok(null);
    }

    @PutMapping("/read-all")
    public Result<?> markAllRead() {
        notificationService.markAllRead();
        return Result.ok(null);
    }
}
