package com.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class ReportHandleRequest {
    @NotNull
    private Integer status;

    @NotNull
    private Integer handleResult;

    private Integer punishmentType;
    private Integer durationDays;
}
