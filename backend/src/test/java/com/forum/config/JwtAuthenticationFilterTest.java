package com.forum.config;

import com.forum.entity.User;
import com.forum.mapper.UserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter JWT认证过滤器 控制结构单元测试")
class JwtAuthenticationFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UserMapper userMapper;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("extractToken() - Token提取")
    class ExtractToken {

        @Test
        @DisplayName("无 Authorization 头应返回 null (if分支: hasText=false)")
        void noAuthHeader_shouldReturnNull() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Authorization 为空字符串应返回 null (if分支: hasText=false)")
        void emptyAuthHeader_shouldReturnNull() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Authorization 不以 Bearer 开头应返回 null (if分支: startsWith=false)")
        void nonBearerToken_shouldReturnNull() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("有效的 Bearer token 应提取并验证 (if分支: token!=null && valid=true)")
        void validBearerToken_shouldExtractAndValidate() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer valid-token-12345");
            when(jwtUtil.validateToken("valid-token-12345")).thenReturn(true);
            when(jwtUtil.getUserIdFromToken("valid-token-12345")).thenReturn(1L);

            User user = new User();
            user.setUserId(1L);
            user.setIsBanned(0);
            user.setIsDeleted(0);
            when(userMapper.selectById(1L)).thenReturn(user);

            filter.doFilterInternal(request, response, filterChain);

            verify(jwtUtil).validateToken("valid-token-12345");
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("doFilterInternal() - 用户认证控制结构")
    class DoFilterInternal {

        @Test
        @DisplayName("Token有效但用户不存在应不设置认证 (if: user!=null=false)")
        void validTokenButNoUser_shouldNotSetAuth() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer token");
            when(jwtUtil.validateToken("token")).thenReturn(true);
            when(jwtUtil.getUserIdFromToken("token")).thenReturn(1L);
            when(userMapper.selectById(1L)).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Token有效但用户被封禁应不设置认证 (if: isBanned!=0=false)")
        void validTokenButBannedUser_shouldNotSetAuth() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer token");
            when(jwtUtil.validateToken("token")).thenReturn(true);
            when(jwtUtil.getUserIdFromToken("token")).thenReturn(1L);

            User user = new User();
            user.setUserId(1L);
            user.setIsBanned(1);
            user.setIsDeleted(0);
            when(userMapper.selectById(1L)).thenReturn(user);

            filter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Token有效但用户已删除应不设置认证 (if: isDeleted!=0=false)")
        void validTokenButDeletedUser_shouldNotSetAuth() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer token");
            when(jwtUtil.validateToken("token")).thenReturn(true);
            when(jwtUtil.getUserIdFromToken("token")).thenReturn(1L);

            User user = new User();
            user.setUserId(1L);
            user.setIsBanned(0);
            user.setIsDeleted(1);
            when(userMapper.selectById(1L)).thenReturn(user);

            filter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Token无效应跳过认证 (if: validateToken=false)")
        void invalidToken_shouldSkipAuth() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
            when(jwtUtil.validateToken("bad-token")).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            verify(jwtUtil, never()).getUserIdFromToken(anyString());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("用户认证等级>=3应赋予ADMIN角色 (if: verificationLevel>=3=true)")
        void adminUser_shouldGetAdminRole() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer admin-token");
            when(jwtUtil.validateToken("admin-token")).thenReturn(true);
            when(jwtUtil.getUserIdFromToken("admin-token")).thenReturn(1L);

            User admin = new User();
            admin.setUserId(1L);
            admin.setIsBanned(0);
            admin.setIsDeleted(0);
            admin.setVerificationLevel(5);
            when(userMapper.selectById(1L)).thenReturn(admin);

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertTrue(auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ADMIN")));
        }

        @Test
        @DisplayName("普通用户(verificationLevel<3)不应有ADMIN角色 (if: verificationLevel>=3=false)")
        void normalUser_shouldNotGetAdminRole() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer user-token");
            when(jwtUtil.validateToken("user-token")).thenReturn(true);
            when(jwtUtil.getUserIdFromToken("user-token")).thenReturn(2L);

            User normalUser = new User();
            normalUser.setUserId(2L);
            normalUser.setIsBanned(0);
            normalUser.setIsDeleted(0);
            normalUser.setVerificationLevel(0);
            when(userMapper.selectById(2L)).thenReturn(normalUser);

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertTrue(auth.getAuthorities().isEmpty());
        }

        @Test
        @DisplayName("verificationLevel为null应不赋予ADMIN角色 (if: null>=3=false)")
        void nullVerificationLevel_shouldNotGetAdminRole() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer token");
            when(jwtUtil.validateToken("token")).thenReturn(true);
            when(jwtUtil.getUserIdFromToken("token")).thenReturn(1L);

            User user = new User();
            user.setUserId(1L);
            user.setIsBanned(0);
            user.setIsDeleted(0);
            user.setVerificationLevel(null);
            when(userMapper.selectById(1L)).thenReturn(user);

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertTrue(auth.getAuthorities().isEmpty());
        }

        @Test
        @DisplayName("verificationLevel=3边界值应赋予ADMIN角色")
        void verificationLevel3_shouldGetAdminRole() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer token");
            when(jwtUtil.validateToken("token")).thenReturn(true);
            when(jwtUtil.getUserIdFromToken("token")).thenReturn(1L);

            User user = new User();
            user.setUserId(1L);
            user.setIsBanned(0);
            user.setIsDeleted(0);
            user.setVerificationLevel(3);
            when(userMapper.selectById(1L)).thenReturn(user);

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertTrue(auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ADMIN")));
        }

        @Test
        @DisplayName("无论认证是否成功 filterChain 都应继续执行")
        void filterChainAlwaysProceeds() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
        }
    }
}
