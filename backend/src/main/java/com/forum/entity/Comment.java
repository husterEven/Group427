package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long commentId;

    private Long postId;

    private Long parentCommentId;

    private Long authorId;

    private String content;

    private Integer likeCount;

    private Integer auditStatus;

    @TableLogic
    private Integer isDeleted;

    private LocalDateTime publishTime;

    @TableField(exist = false)
    private User author;

    @TableField(exist = false)
    private Boolean isLiked;

    @TableField(exist = false)
    private Integer replyCount;
}
