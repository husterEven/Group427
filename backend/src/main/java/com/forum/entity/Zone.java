package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("zone")
public class Zone {

    @TableId(type = IdType.AUTO)
    private Integer zoneId;

    private String zoneName;

    private Integer sectionId;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}
