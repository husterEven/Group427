package com.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class DynamicCreateRequest {
    @NotBlank
    private String content;
}
