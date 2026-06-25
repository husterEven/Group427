package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("private_message")
public class PrivateMessage {
    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Integer isRead;
    @TableLogic
    private Integer isDeleted;
    private LocalDateTime sendTime;

    @TableField(exist = false)
    private Boolean isMine;
}
