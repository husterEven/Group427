package com.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("vote_record")
public class VoteRecord {
    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;
    private Long voteId;
    private Long userId;
    private Integer optionIndex;
    private LocalDateTime voteTime;
}
