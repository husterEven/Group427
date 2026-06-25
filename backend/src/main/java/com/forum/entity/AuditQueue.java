package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("audit_queue")
public class AuditQueue {

    @TableId(type = IdType.AUTO)
    private Long auditItemId;

    private Integer contentType;

    private Long contentId;

    private String preview;

    private Long submitterId;

    private Integer auditStatus;

    private Long auditorId;

    private String auditComment;

    private LocalDateTime createdAt;

    private LocalDateTime auditedAt;
}
