package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("follow")
public class Follow {

    @TableId(type = IdType.AUTO)
    private Long relationId;

    private Long followerId;

    private Long followeeId;

    private Integer isStarred;

    private LocalDateTime createdAt;

    @TableField(exist = false)
    private Long userId;

    @TableField(exist = false)
    private String nickname;

    @TableField(exist = false)
    private String avatarUrl;

    @TableField(exist = false)
    private String bio;

    @TableField(exist = false)
    private Boolean isMutual;
}
