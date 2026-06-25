package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("attachment")
public class Attachment {
    @TableId(value = "attachment_id", type = IdType.AUTO)
    private Long attachmentId;
    private Long postId;
    private Long userId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private Integer fileType;
    private Integer auditStatus;
    private LocalDateTime createdAt;
}
