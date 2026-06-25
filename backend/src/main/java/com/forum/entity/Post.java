package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long postId;

    private Long authorId;

    private String title;

    private String content;

    private Integer contentType;

    private Integer sectionId;

    private Integer zoneId;

    private Integer auditStatus;

    private Integer likeCount;

    private Integer viewCount;

    private Integer commentCount;

    private Integer collectCount;

    @TableLogic
    private Integer isDeleted;

    private Integer isEssence;

    private Integer isPinned;

    private LocalDateTime publishTime;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private User author;

    @TableField(exist = false)
    private String sectionName;

    @TableField(exist = false)
    private String zoneName;

    @TableField(exist = false)
    private Boolean isLiked;

    @TableField(exist = false)
    private Boolean isCollected;

    @TableField(exist = false)
    private Object vote;
}
