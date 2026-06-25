package com.forum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.common.GlobalExceptionHandler;
import com.forum.dto.LoginRequest;
import com.forum.dto.RefreshTokenRequest;
import com.forum.dto.RegisterRequest;
import com.forum.dto.TokenResponse;
import com.forum.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 认证接口 单元测试")
class AuthControllerTest {

    @Mock private AuthService authService;
    @InjectMocks private AuthController authController;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private TokenResponse createTokenResponse() {
        TokenResponse resp = new TokenResponse();
        resp.setAccessToken("access-xxx");
        resp.setRefreshToken("refresh-xxx");
        resp.setTokenType("Bearer");
        resp.setExpiresIn(7200L);
        return resp;
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("有效请求应返回 200 和 TokenResponse")
        void validRequest_shouldReturnToken() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setNickname("test");
            req.setAccount("13800138000");
            req.setPassword("123456");

            when(authService.register(any(RegisterRequest.class))).thenReturn(createTokenResponse());

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("注册成功"))
                    .andExpect(jsonPath("$.data.accessToken").value("access-xxx"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-xxx"));

            verify(authService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("昵称为空(少于2字符)应返回 400")
        void invalidNickname_shouldReturn400() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setNickname("a");
            req.setAccount("13800138000");
            req.setPassword("123456");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("账号为空应返回 400")
        void blankAccount_shouldReturn400() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setNickname("test");
            req.setAccount("");
            req.setPassword("123456");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("密码为空应返回 400")
        void blankPassword_shouldReturn400() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setNickname("test");
            req.setAccount("13800138000");
            req.setPassword("");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Service 抛出 RuntimeException 时应返回错误状态")
        void serviceThrowsException_shouldPropagate() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setNickname("test");
            req.setAccount("existing@test.com");
            req.setPassword("123456");

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new RuntimeException("账号已存在"));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("账号已存在"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("有效账号密码应返回 200 和 Token")
        void validLogin_shouldReturnToken() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setAccount("test@test.com");
            req.setPassword("correct");

            when(authService.login(any(LoginRequest.class))).thenReturn(createTokenResponse());

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("登录成功"))
                    .andExpect(jsonPath("$.data.accessToken").value("access-xxx"));
        }

        @Test
        @DisplayName("账号为空应返回 400")
        void blankAccount_shouldReturn400() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setAccount("");
            req.setPassword("password");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("密码错误应抛出异常")
        void wrongPassword_shouldThrow() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setAccount("test@test.com");
            req.setPassword("wrong");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new RuntimeException("账号或密码错误"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("账号或密码错误"));
        }

        @Test
        @DisplayName("被封禁用户登录应返回错误")
        void bannedUser_shouldReturnError() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setAccount("banned@test.com");
            req.setPassword("password");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new RuntimeException("账号已被封禁"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("账号已被封禁"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("有效 refreshToken 应返回新 Token")
        void validRefreshToken_shouldReturnNewToken() throws Exception {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("valid-refresh-token");

            when(authService.refresh(any(RefreshTokenRequest.class))).thenReturn(createTokenResponse());

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Token刷新成功"))
                    .andExpect(jsonPath("$.data.accessToken").value("access-xxx"));
        }

        @Test
        @DisplayName("无效 refreshToken 应抛出异常")
        void invalidRefreshToken_shouldThrow() throws Exception {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("invalid");

            when(authService.refresh(any(RefreshTokenRequest.class)))
                    .thenThrow(new RuntimeException("refreshToken无效或已过期"));

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("refreshToken无效或已过期"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("登出应返回 200")
        void logout_shouldReturn200() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("已登出"));

            verify(authService).logout();
        }
    }
}
