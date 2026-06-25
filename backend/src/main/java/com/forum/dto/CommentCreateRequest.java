package com.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class CommentCreateRequest {
    @NotBlank
    private String content;

    private Long parentCommentId;
}
