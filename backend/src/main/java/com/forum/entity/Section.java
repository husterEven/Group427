package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("section")
public class Section {

    @TableId(type = IdType.AUTO)
    private Integer sectionId;

    private String sectionName;

    private Integer sectionType;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    @TableField(exist = false)
    private List<Zone> zones;
}
