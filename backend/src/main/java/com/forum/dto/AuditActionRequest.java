package com.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class AuditActionRequest {
    @NotNull
    private Integer auditStatus;

    private String auditComment;
}
