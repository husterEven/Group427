package com.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_verification")
public class UserVerification {

    @TableId(type = IdType.AUTO)
    private Long recordId;

    private Long userId;

    private Integer verificationType;

    private Integer auditStatus;

    private LocalDateTime createdAt;
}
