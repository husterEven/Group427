package com.forum.service.impl;

import com.forum.common.SecurityUtil;
import com.forum.config.JwtUtil;
import com.forum.dto.LoginRequest;
import com.forum.dto.RefreshTokenRequest;
import com.forum.dto.RegisterRequest;
import com.forum.dto.TokenResponse;
import com.forum.entity.PrivacySetting;
import com.forum.entity.User;
import com.forum.entity.UserAchievement;
import com.forum.entity.UserPreference;
import com.forum.mapper.PrivacySettingMapper;
import com.forum.mapper.UserAchievementMapper;
import com.forum.mapper.UserMapper;
import com.forum.mapper.UserPreferenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl 认证服务 单元测试")
class AuthServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private UserPreferenceMapper userPreferenceMapper;
    @Mock private PrivacySettingMapper privacySettingMapper;
    @Mock private UserAchievementMapper userAchievementMapper;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final String ACCESS_TOKEN = "access-token-xxx";
    private static final String REFRESH_TOKEN = "refresh-token-xxx";

    @BeforeEach
    void setUp() {
        lenient().when(jwtUtil.generateAccessToken(anyLong())).thenReturn(ACCESS_TOKEN);
        lenient().when(jwtUtil.generateRefreshToken(anyLong())).thenReturn(REFRESH_TOKEN);
    }

    @Nested
    @DisplayName("register() 注册")
    class Register {

        @Test
        @DisplayName("新用户注册应成功并返回 TokenResponse")
        void newUser_shouldRegisterSuccessfully() {
            RegisterRequest req = new RegisterRequest();
            req.setNickname("小明");
            req.setAccount("13800138000");
            req.setPassword("123456");

            when(userMapper.selectByAccount("13800138000")).thenReturn(null);
            when(passwordEncoder.encode("123456")).thenReturn("hashed_pwd");
            when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setUserId(1L);
                return 1;
            });

            TokenResponse resp = authService.register(req);

            assertNotNull(resp);
            assertEquals(ACCESS_TOKEN, resp.getAccessToken());
            assertEquals(REFRESH_TOKEN, resp.getRefreshToken());

            verify(userMapper).insert(any(User.class));
            verify(userPreferenceMapper).insert(any(UserPreference.class));
            verify(privacySettingMapper).insert(any(PrivacySetting.class));
            verify(userAchievementMapper).insert(any(UserAchievement.class));
        }

        @Test
        @DisplayName("账号已存在应抛出 RuntimeException")
        void duplicateAccount_shouldThrowRuntimeException() {
            RegisterRequest req = new RegisterRequest();
            req.setNickname("test");
            req.setAccount("existing@test.com");
            req.setPassword("123456");

            when(userMapper.selectByAccount("existing@test.com")).thenReturn(new User());

            RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(req));
            assertEquals("账号已存在", ex.getMessage());
        }

        @Test
        @DisplayName("邮箱注册应将 account 存入 email 字段")
        void emailRegistration_shouldSetEmailField() {
            RegisterRequest req = new RegisterRequest();
            req.setNickname("test");
            req.setAccount("test@example.com");
            req.setPassword("123456");

            when(userMapper.selectByAccount("test@example.com")).thenReturn(null);
            when(passwordEncoder.encode("123456")).thenReturn("hashed");
            when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setUserId(1L);
                return 1;
            });

            authService.register(req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userMapper).insert(captor.capture());
            User saved = captor.getValue();
            assertEquals("test@example.com", saved.getEmail());
            assertNull(saved.getMobile());
        }

        @Test
        @DisplayName("手机号注册应将 account 存入 mobile 字段")
        void mobileRegistration_shouldSetMobileField() {
            RegisterRequest req = new RegisterRequest();
            req.setNickname("test");
            req.setAccount("13800138000");
            req.setPassword("123456");

            when(userMapper.selectByAccount("13800138000")).thenReturn(null);
            when(passwordEncoder.encode("123456")).thenReturn("hashed");
            when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setUserId(1L);
                return 1;
            });

            authService.register(req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userMapper).insert(captor.capture());
            User saved = captor.getValue();
            assertEquals("13800138000", saved.getMobile());
            assertNull(saved.getEmail());
        }

        @Test
        @DisplayName("注册用户默认不应被禁言")
        void newUser_shouldNotBeBanned() {
            RegisterRequest req = new RegisterRequest();
            req.setNickname("test");
            req.setAccount("test@test.com");
            req.setPassword("123456");

            when(userMapper.selectByAccount("test@test.com")).thenReturn(null);
            when(passwordEncoder.encode("123456")).thenReturn("hashed");
            when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setUserId(1L);
                return 1;
            });

            authService.register(req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userMapper).insert(captor.capture());
            assertEquals(0, captor.getValue().getIsBanned());
        }
    }

    @Nested
    @DisplayName("login() 登录")
    class Login {

        @Test
        @DisplayName("正确的账号密码应登录成功")
        void validCredentials_shouldLoginSuccessfully() {
            LoginRequest req = new LoginRequest();
            req.setAccount("test@test.com");
            req.setPassword("correct");

            User user = new User();
            user.setUserId(1L);
            user.setPasswordHash("hashed_pwd");
            user.setIsBanned(0);

            when(userMapper.selectByAccount("test@test.com")).thenReturn(user);
            when(passwordEncoder.matches("correct", "hashed_pwd")).thenReturn(true);

            TokenResponse resp = authService.login(req);

            assertNotNull(resp);
            assertEquals(ACCESS_TOKEN, resp.getAccessToken());
            assertEquals(REFRESH_TOKEN, resp.getRefreshToken());
        }

        @Test
        @DisplayName("账号不存在应抛出 RuntimeException")
        void nonExistentAccount_shouldThrowRuntimeException() {
            LoginRequest req = new LoginRequest();
            req.setAccount("nouser@test.com");
            req.setPassword("password");

            when(userMapper.selectByAccount("nouser@test.com")).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(req));
            assertEquals("账号或密码错误", ex.getMessage());
        }

        @Test
        @DisplayName("密码错误应抛出 RuntimeException")
        void wrongPassword_shouldThrowRuntimeException() {
            LoginRequest req = new LoginRequest();
            req.setAccount("test@test.com");
            req.setPassword("wrong");

            User user = new User();
            user.setUserId(1L);
            user.setPasswordHash("hashed_pwd");

            when(userMapper.selectByAccount("test@test.com")).thenReturn(user);
            when(passwordEncoder.matches("wrong", "hashed_pwd")).thenReturn(false);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(req));
            assertEquals("账号或密码错误", ex.getMessage());
        }

        @Test
        @DisplayName("被封禁用户登录应抛出 RuntimeException")
        void bannedUser_shouldThrowRuntimeException() {
            LoginRequest req = new LoginRequest();
            req.setAccount("banned@test.com");
            req.setPassword("password");

            User user = new User();
            user.setUserId(1L);
            user.setPasswordHash("hashed");
            user.setIsBanned(1);

            when(userMapper.selectByAccount("banned@test.com")).thenReturn(user);
            when(passwordEncoder.matches("password", "hashed")).thenReturn(true);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(req));
            assertEquals("账号已被封禁", ex.getMessage());
        }

        @Test
        @DisplayName("已删除用户登录应抛出 RuntimeException")
        void deletedUser_shouldThrowRuntimeException() {
            LoginRequest req = new LoginRequest();
            req.setAccount("deleted@test.com");
            req.setPassword("password");

            User user = new User();
            user.setUserId(1L);
            user.setPasswordHash("hashed");
            user.setIsBanned(0);
            user.setIsDeleted(1);

            when(userMapper.selectByAccount("deleted@test.com")).thenReturn(user);
            when(passwordEncoder.matches("password", "hashed")).thenReturn(true);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(req));
            assertEquals("账号不存在", ex.getMessage());
        }

        @Test
        @DisplayName("用户无封禁标记(isBanned=null)应正常登录")
        void nullBannedStatus_shouldLoginSuccessfully() {
            LoginRequest req = new LoginRequest();
            req.setAccount("test@test.com");
            req.setPassword("password");

            User user = new User();
            user.setUserId(1L);
            user.setPasswordHash("hashed");
            user.setIsBanned(null);

            when(userMapper.selectByAccount("test@test.com")).thenReturn(user);
            when(passwordEncoder.matches("password", "hashed")).thenReturn(true);

            TokenResponse resp = authService.login(req);
            assertNotNull(resp);
        }
    }

    @Nested
    @DisplayName("refresh() 刷新 Token")
    class Refresh {

        @Test
        @DisplayName("有效的 refreshToken 应返回新 token")
        void validRefreshToken_shouldReturnNewTokens() {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken(REFRESH_TOKEN);

            when(jwtUtil.validateToken(REFRESH_TOKEN)).thenReturn(true);
            when(jwtUtil.getUserIdFromToken(REFRESH_TOKEN)).thenReturn(1L);

            TokenResponse resp = authService.refresh(req);

            assertNotNull(resp);
            assertEquals(ACCESS_TOKEN, resp.getAccessToken());
            assertEquals(REFRESH_TOKEN, resp.getRefreshToken());
        }

        @Test
        @DisplayName("无效的 refreshToken 应抛出 RuntimeException")
        void invalidRefreshToken_shouldThrowRuntimeException() {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("invalid-token");

            when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.refresh(req));
            assertEquals("refreshToken无效或已过期", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("logout() 登出")
    class Logout {

        @Test
        @DisplayName("logout 不应抛异常 (无状态 JWT)")
        void logout_shouldNotThrow() {
            assertDoesNotThrow(() -> authService.logout());
        }
    }
}
