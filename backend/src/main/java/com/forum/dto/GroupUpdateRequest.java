package com.forum.dto;

import lombok.Data;

@Data
public class GroupUpdateRequest {
    private String groupName;
    private Integer mode;
    private Integer status;
}
