package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_behavior")
public class UserBehavior {

    @TableId(type = IdType.AUTO)
    private Long behaviorId;

    private Long userId;

    private Integer behaviorType;

    private Long targetId;

    private LocalDateTime createdAt;
}
