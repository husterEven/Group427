package com.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("group_post")
public class GroupPost {

    @TableId(type = IdType.AUTO)
    private Long groupPostId;

    private Long groupId;

    private Long authorId;

    private String content;

    private Integer likeCount;

    @TableLogic
    private Integer isDeleted;

    private LocalDateTime publishTime;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Object author;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Boolean isLiked;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Boolean isMine;
}
