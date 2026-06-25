package com.forum.dto;

import lombok.Data;

@Data
public class PostQueryRequest {
    private Integer page = 1;
    private Integer pageSize = 20;
    private String sort = "latest";
    private Integer sectionId;
    private Integer zoneId;
    private String keyword;
    private Boolean isEssence;
    private Long authorId;
}
