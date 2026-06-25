package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long userId;

    private String nickname;

    @JsonIgnore
    private String passwordHash;

    private String mobile;

    private String email;

    private String avatarUrl;

    private String bio;

    private Integer gender;

    private Integer verificationLevel;

    private Integer points;

    private Integer level;

    private Integer isBanned;

    @TableLogic
    private Integer isDeleted;

    private String registerIp;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private Boolean isFollowed;

    @TableField(exist = false)
    private Integer followerCount;

    @TableField(exist = false)
    private Integer followingCount;
}
