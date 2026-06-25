package com.forum.config;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil 单元测试")
class JwtUtilTest {

    private static final String SECRET = "YTk3ZDg2ZjFiMmMzZDQ1ZTY3ODkwYWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVm";
    private static final long ACCESS_EXP = 7200000L;
    private static final long REFRESH_EXP = 604800000L;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, ACCESS_EXP, REFRESH_EXP);
    }

    @Nested
    @DisplayName("Token 生成")
    class TokenGeneration {

        @Test
        @DisplayName("生成 accessToken 应包含 userId 且不为空")
        void generateAccessToken_shouldReturnNonEmptyToken() {
            String token = jwtUtil.generateAccessToken(1L);
            assertNotNull(token);
            assertTrue(token.length() > 0);
        }

        @Test
        @DisplayName("生成 refreshToken 应包含 userId 且不为空")
        void generateRefreshToken_shouldReturnNonEmptyToken() {
            String token = jwtUtil.generateRefreshToken(1L);
            assertNotNull(token);
            assertTrue(token.length() > 0);
        }

        @Test
        @DisplayName("accessToken 应为 JWT 三段式格式")
        void generateAccessToken_shouldBeValidJwtFormat() {
            String token = jwtUtil.generateAccessToken(100L);
            String[] parts = token.split("\\.");
            assertEquals(3, parts.length);
        }

        @Test
        @DisplayName("不同 userId 生成的 token 应不同")
        void differentUserIds_shouldGenerateDifferentTokens() {
            String token1 = jwtUtil.generateAccessToken(1L);
            String token2 = jwtUtil.generateAccessToken(2L);
            assertNotEquals(token1, token2);
        }
    }

    @Nested
    @DisplayName("Token 解析 - getUserIdFromToken")
    class GetUserIdFromToken {

        @Test
        @DisplayName("有效 token 应正确解析出 userId")
        void validToken_shouldReturnUserId() {
            String token = jwtUtil.generateAccessToken(42L);
            Long userId = jwtUtil.getUserIdFromToken(token);
            assertEquals(42L, userId);
        }

        @Test
        @DisplayName("refreshToken 也应正确解析出 userId")
        void refreshToken_shouldReturnUserId() {
            String token = jwtUtil.generateRefreshToken(99L);
            Long userId = jwtUtil.getUserIdFromToken(token);
            assertEquals(99L, userId);
        }

        @Test
        @DisplayName("无效/篡改的 token 应抛出异常")
        void tamperedToken_shouldThrowException() {
            assertThrows(JwtException.class, () ->
                    jwtUtil.getUserIdFromToken("invalid.token.here"));
        }

        @Test
        @DisplayName("空字符串 token 应抛出异常")
        void emptyToken_shouldThrowException() {
            assertThrows(Exception.class, () ->
                    jwtUtil.getUserIdFromToken(""));
        }
    }

    @Nested
    @DisplayName("Token 校验 - validateToken")
    class ValidateToken {

        @Test
        @DisplayName("有效的 accessToken 应返回 true")
        void validAccessToken_shouldReturnTrue() {
            String token = jwtUtil.generateAccessToken(1L);
            assertTrue(jwtUtil.validateToken(token));
        }

        @Test
        @DisplayName("有效的 refreshToken 应返回 true")
        void validRefreshToken_shouldReturnTrue() {
            String token = jwtUtil.generateRefreshToken(1L);
            assertTrue(jwtUtil.validateToken(token));
        }

        @Test
        @DisplayName("空字符串 token 应返回 false")
        void emptyToken_shouldReturnFalse() {
            assertFalse(jwtUtil.validateToken(""));
        }

        @Test
        @DisplayName("无效的 token 应返回 false")
        void invalidToken_shouldReturnFalse() {
            assertFalse(jwtUtil.validateToken("eyJhbGciOiJIUzI1NiJ9.invalid.signature"));
        }

        @Test
        @DisplayName("null token 应返回 false")
        void nullToken_shouldReturnFalse() {
            assertFalse(jwtUtil.validateToken(null));
        }

        @Test
        @DisplayName("格式错误的 token 应返回 false")
        void malformedToken_shouldReturnFalse() {
            assertFalse(jwtUtil.validateToken("not-a-jwt"));
        }
    }
}
