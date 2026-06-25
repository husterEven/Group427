package com.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_preference")
public class UserPreference {

    @TableId(type = IdType.AUTO)
    private Long preferenceId;

    private Long userId;

    private String focusMarkets;

    private String riskType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
