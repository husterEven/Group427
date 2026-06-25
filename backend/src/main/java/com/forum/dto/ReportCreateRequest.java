package com.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class ReportCreateRequest {
    @NotNull
    private Integer targetType;

    @NotNull
    private Long targetId;

    @Size(max = 500)
    private String reason;
}
