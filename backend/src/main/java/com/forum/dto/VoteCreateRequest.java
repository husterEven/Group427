package com.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Data
public class VoteCreateRequest {
    @NotBlank
    @Size(max = 200)
    private String voteTitle;

    @Size(min = 2, max = 10)
    private List<String> options;

    private String endTime;
}
