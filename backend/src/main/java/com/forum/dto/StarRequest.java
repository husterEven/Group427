package com.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class StarRequest {
    @NotNull
    private Boolean isStarred;
}
