package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_punishment")
public class UserPunishment {

    @TableId(type = IdType.AUTO)
    private Long punishmentId;

    private Long userId;

    private Integer punishmentType;

    private String reason;

    private Long operatorId;

    private Integer durationDays;

    private Integer isActive;

    private LocalDateTime createdAt;

    private LocalDateTime expireAt;
}
