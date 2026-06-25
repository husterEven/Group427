package com.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class PostUpdateRequest {
    @Size(max = 200)
    private String title;

    private String content;
    private Integer sectionId;
    private Integer zoneId;
}
