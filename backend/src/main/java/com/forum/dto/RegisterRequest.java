package com.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class RegisterRequest {
    @NotBlank
    @Size(min = 2, max = 20)
    private String nickname;

    @NotBlank
    private String account;

    @NotBlank
    @Size(min = 6, max = 32)
    private String password;
}
