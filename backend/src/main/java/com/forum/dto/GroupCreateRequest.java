package com.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class GroupCreateRequest {
    @NotBlank
    private String groupName;

    private Integer mode = 0;
}
