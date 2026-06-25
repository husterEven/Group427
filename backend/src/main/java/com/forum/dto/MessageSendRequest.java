package com.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class MessageSendRequest {
    @NotBlank
    private String content;
}
