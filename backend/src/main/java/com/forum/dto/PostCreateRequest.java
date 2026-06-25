package com.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Data
public class PostCreateRequest {
    @NotBlank
    @Size(max = 200)
    private String title;

    private String content;

    private Integer contentType = 0;
    private Integer sectionId;
    private Integer zoneId;
    private List<Long> attachmentIds;
}
