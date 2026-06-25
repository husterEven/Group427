package com.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("group_member")
public class GroupMember {

    @TableId(type = IdType.AUTO)
    private Long memberId;

    private Long groupId;

    private Long userId;

    private Integer role;

    private LocalDateTime joinedAt;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String nickname;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String avatarUrl;
}
