package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_info")
public class GroupInfo {
    @TableId(value = "group_id", type = IdType.AUTO)
    private Long groupId;
    private String groupName;
    private Long ownerId;
    private Integer mode;
    private Integer status;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private Object owner;

    @TableField(exist = false)
    private Integer memberCount;

    @TableField(exist = false)
    private Integer myRole;
}
