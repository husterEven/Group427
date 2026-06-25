package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("realtime_dynamic")
public class RealtimeDynamic {
    @TableId(value = "dynamic_id", type = IdType.AUTO)
    private Long dynamicId;
    private Long authorId;
    private String content;
    private Integer likeCount;
    @TableLogic
    private Integer isDeleted;
    private LocalDateTime publishTime;
}
