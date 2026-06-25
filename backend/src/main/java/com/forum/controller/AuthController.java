package com.forum.controller;

import com.forum.common.Result;
import com.forum.dto.LoginRequest;
import com.forum.dto.RefreshTokenRequest;
import com.forum.dto.RegisterRequest;
import com.forum.dto.TokenResponse;
import com.forum.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        TokenResponse tokenResponse = authService.register(request);
        return Result.ok("注册成功", tokenResponse);
    }

    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return Result.ok("登录成功", tokenResponse);
    }

    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse tokenResponse = authService.refresh(request);
        return Result.ok("Token刷新成功", tokenResponse);
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        authService.logout();
        return Result.ok("已登出", null);
    }
}
