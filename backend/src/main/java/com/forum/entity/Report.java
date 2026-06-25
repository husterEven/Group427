package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long reportId;

    private Long reporterId;

    private Integer targetType;

    private Long targetId;

    private String reason;

    private Integer status;

    private Long handlerId;

    private Integer handleResult;

    private LocalDateTime createdAt;

    private LocalDateTime handledAt;
}
