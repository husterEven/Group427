package com.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class RiskAssessmentDTO {
    @NotBlank
    private String resultLevel;
}
