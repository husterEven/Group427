package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("post_like")
public class PostLike {

    @TableId(type = IdType.AUTO)
    private Long likeId;

    private Long userId;

    private Long postId;

    private LocalDateTime createdAt;
}
