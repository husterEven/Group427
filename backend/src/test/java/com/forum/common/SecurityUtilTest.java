package com.forum.common;

import com.forum.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecurityUtil 安全工具 单元测试")
class SecurityUtilTest {

    private SecurityUtil securityUtil;

    @BeforeEach
    void setUp() {
        securityUtil = new SecurityUtil();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getCurrentUser()")
    class GetCurrentUser {

        @Test
        @DisplayName("有认证用户时应返回 User 对象")
        void authenticatedUser_shouldReturnUser() {
            User user = new User();
            user.setUserId(1L);
            user.setNickname("testUser");
            Authentication auth = new UsernamePasswordAuthenticationToken(user, null);
            SecurityContextHolder.getContext().setAuthentication(auth);

            User result = securityUtil.getCurrentUser();
            assertNotNull(result);
            assertEquals(1L, result.getUserId());
            assertEquals("testUser", result.getNickname());
        }

        @Test
        @DisplayName("未认证时应抛出 RuntimeException")
        void noAuthentication_shouldThrowRuntimeException() {
            assertThrows(RuntimeException.class, () -> securityUtil.getCurrentUser());
        }

        @Test
        @DisplayName("认证对象不是 User 类型时应抛出 RuntimeException")
        void nonUserPrincipal_shouldThrowRuntimeException() {
            Authentication auth = new UsernamePasswordAuthenticationToken("stringPrincipal", null);
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThrows(RuntimeException.class, () -> securityUtil.getCurrentUser());
        }

        @Test
        @DisplayName("auth 为 null 时应抛出 RuntimeException")
        void nullAuthentication_shouldThrowRuntimeException() {
            assertThrows(RuntimeException.class, () -> securityUtil.getCurrentUser());
        }
    }

    @Nested
    @DisplayName("getCurrentUserId()")
    class GetCurrentUserId {

        @Test
        @DisplayName("有认证用户时应返回 userId")
        void authenticatedUser_shouldReturnUserId() {
            User user = new User();
            user.setUserId(99L);
            Authentication auth = new UsernamePasswordAuthenticationToken(user, null);
            SecurityContextHolder.getContext().setAuthentication(auth);

            Long userId = securityUtil.getCurrentUserId();
            assertEquals(99L, userId);
        }

        @Test
        @DisplayName("未认证时应抛出 RuntimeException")
        void noAuthentication_shouldThrowRuntimeException() {
            assertThrows(RuntimeException.class, () -> securityUtil.getCurrentUserId());
        }
    }
}
