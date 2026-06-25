package com.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

@Data
public class UserUpdateRequest {
    @Size(min = 2, max = 20)
    private String nickname;

    private String avatarUrl;

    @Size(max = 200)
    private String bio;

    @Min(0)
    @Max(2)
    private Integer gender;
}
