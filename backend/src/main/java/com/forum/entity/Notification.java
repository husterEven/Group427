package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long notificationId;

    private Long userId;

    private Integer notifyType;

    private String title;

    private String content;

    private Integer targetType;

    private Long targetId;

    private Integer isRead;

    private LocalDateTime createdAt;
}
