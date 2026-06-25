package com.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("privacy_setting")
public class PrivacySetting {

    @TableId(type = IdType.AUTO)
    private Long settingId;

    private Long userId;

    private Integer profileVisibility;

    private LocalDateTime updatedAt;
}
