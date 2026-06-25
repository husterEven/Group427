package com.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class EssenceRequest {
    @NotNull
    private Boolean isEssence;
}
