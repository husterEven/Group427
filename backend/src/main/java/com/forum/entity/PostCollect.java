package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("post_collect")
public class PostCollect {

    @TableId(type = IdType.AUTO)
    private Long collectId;

    private Long userId;

    private Long postId;

    private LocalDateTime createdAt;
}
