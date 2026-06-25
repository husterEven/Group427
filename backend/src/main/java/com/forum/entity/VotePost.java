package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("vote_post")
public class VotePost {
    @TableId(value = "vote_id", type = IdType.AUTO)
    private Long voteId;
    private Long postId;
    private String voteTitle;
    private LocalDateTime endTime;

    private String optionsJson;
}
