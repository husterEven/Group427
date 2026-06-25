package com.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("risk_assessment_answer")
public class RiskAssessmentAnswer {

    @TableId(type = IdType.AUTO)
    private Long answerId;

    private Long userId;

    private String resultLevel;

    private LocalDateTime completeTime;
}
