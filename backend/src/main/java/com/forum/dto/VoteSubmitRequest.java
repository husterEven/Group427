package com.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class VoteSubmitRequest {
    @NotNull
    private Integer optionIndex;
}
