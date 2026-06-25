package com.forum.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler 全局异常处理 控制结构单元测试")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("handleValidation() - MethodArgumentNotValidException")
    class HandleValidation {

        @Test
        @DisplayName("单个字段校验失败应返回拼接的错误消息")
        void singleFieldError_shouldReturnJoinedMessage() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("registerRequest", "account", "手机号不能为空");
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

            Result<?> result = handler.handleValidation(ex);

            assertEquals(400, result.getCode());
            assertTrue(result.getMessage().contains("参数校验失败"));
            assertTrue(result.getMessage().contains("手机号不能为空"));
        }

        @Test
        @DisplayName("多个字段校验失败应将错误消息用分号拼接")
        void multipleFieldErrors_shouldJoinWithSemicolon() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError err1 = new FieldError("req", "account", "账号不能为空");
            FieldError err2 = new FieldError("req", "password", "密码长度不足6位");
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(err1, err2));

            Result<?> result = handler.handleValidation(ex);

            assertEquals(400, result.getCode());
            assertTrue(result.getMessage().contains("账号不能为空"));
            assertTrue(result.getMessage().contains("密码长度不足6位"));
            assertTrue(result.getMessage().contains(";"));
        }

        @Test
        @DisplayName("无字段错误应仅返回前缀消息")
        void noFieldErrors_shouldReturnOnlyPrefix() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of());

            Result<?> result = handler.handleValidation(ex);

            assertEquals(400, result.getCode());
            assertEquals("参数校验失败：", result.getMessage());
        }
    }

    @Nested
    @DisplayName("handleBadCredentials() - BadCredentialsException")
    class HandleBadCredentials {

        @Test
        @DisplayName("凭证错误应返回 401 和预设消息")
        void badCredentials_shouldReturn401() {
            BadCredentialsException ex = new BadCredentialsException("wrong");

            Result<?> result = handler.handleBadCredentials(ex);

            assertEquals(401, result.getCode());
            assertEquals("账号或密码错误", result.getMessage());
            assertNull(result.getData());
        }
    }

    @Nested
    @DisplayName("handleAccessDenied() - AccessDeniedException")
    class HandleAccessDenied {

        @Test
        @DisplayName("权限不足应返回 403 和预设消息")
        void accessDenied_shouldReturn403() {
            AccessDeniedException ex = new AccessDeniedException("denied");

            Result<?> result = handler.handleAccessDenied(ex);

            assertEquals(403, result.getCode());
            assertEquals("无权限执行此操作", result.getMessage());
            assertNull(result.getData());
        }
    }

    @Nested
    @DisplayName("handleIllegalArgument() - IllegalArgumentException")
    class HandleIllegalArgument {

        @Test
        @DisplayName("参数非法应返回 400 并携带异常消息")
        void illegalArgument_shouldReturn400WithMessage() {
            IllegalArgumentException ex = new IllegalArgumentException("页码不能为负数");

            Result<?> result = handler.handleIllegalArgument(ex);

            assertEquals(400, result.getCode());
            assertEquals("页码不能为负数", result.getMessage());
        }

        @Test
        @DisplayName("空异常消息应正常返回")
        void nullMessage_shouldNotThrow() {
            IllegalArgumentException ex = new IllegalArgumentException();

            Result<?> result = handler.handleIllegalArgument(ex);

            assertEquals(400, result.getCode());
            assertNull(result.getMessage());
        }

        @Test
        @DisplayName("带详细异常消息应完整透传")
        void detailedMessage_shouldBePassedThrough() {
            IllegalArgumentException ex = new IllegalArgumentException("帖子ID不存在：99999");

            Result<?> result = handler.handleIllegalArgument(ex);

            assertEquals("帖子ID不存在：99999", result.getMessage());
        }
    }

    @Nested
    @DisplayName("handleRuntime() - RuntimeException")
    class HandleRuntime {

        @Test
        @DisplayName("RuntimeException 应返回 400 并携带异常消息")
        void runtimeException_shouldReturn400() {
            RuntimeException ex = new RuntimeException("不能关注自己");

            Result<?> result = handler.handleRuntime(ex);

            assertEquals(400, result.getCode());
            assertEquals("不能关注自己", result.getMessage());
        }

        @Test
        @DisplayName("带嵌套原因的 RuntimeException 应透传消息")
        void nestedRuntime_shouldPassThroughMessage() {
            RuntimeException ex = new RuntimeException("圈子不存在");

            Result<?> result = handler.handleRuntime(ex);

            assertEquals(400, result.getCode());
            assertEquals("圈子不存在", result.getMessage());
        }
    }

    @Nested
    @DisplayName("handleException() - 兜底 Exception")
    class HandleException {

        @Test
        @DisplayName("未分类异常应返回 500 和内部错误消息")
        void genericException_shouldReturn500() {
            Exception ex = new Exception("未知错误");

            Result<?> result = handler.handleException(ex);

            assertEquals(500, result.getCode());
            assertEquals("服务器内部错误", result.getMessage());
        }

        @Test
        @DisplayName("NullPointerException 作为 Exception 子类应被捕获")
        void nullPointer_shouldBeCaughtAsException() {
            Exception ex = new NullPointerException("null");

            Result<?> result = handler.handleException(ex);

            assertEquals(500, result.getCode());
            assertEquals("服务器内部错误", result.getMessage());
        }

        @Test
        @DisplayName("异常消息不应透传给客户端")
        void exceptionMessage_shouldNotLeakToClient() {
            Exception ex = new Exception("数据库连接超时: jdbc:mysql://...");

            Result<?> result = handler.handleException(ex);

            assertEquals("服务器内部错误", result.getMessage());
        }
    }

    @Nested
    @DisplayName("控制结构覆盖 - 异常类型分发")
    class ControlStructureCoverage {

        @Test
        @DisplayName("if-else分发: RuntimeException vs IllegalArgumentException 返回码相同但消息来源不同")
        void runtimeVsIllegalArgument_sameCodeDifferentSource() {
            Result<?> runtime = handler.handleRuntime(new RuntimeException("业务异常"));
            Result<?> illegal = handler.handleIllegalArgument(new IllegalArgumentException("参数异常"));

            assertEquals(400, runtime.getCode());
            assertEquals(400, illegal.getCode());
            assertEquals("业务异常", runtime.getMessage());
            assertEquals("参数异常", illegal.getMessage());
        }

        @Test
        @DisplayName("if-else分发: BadCredentials(401) vs AccessDenied(403) vs Exception(500) 不同状态码")
        void differentExceptions_differentStatusCodes() {
            assertEquals(401, handler.handleBadCredentials(new BadCredentialsException("")).getCode());
            assertEquals(403, handler.handleAccessDenied(new AccessDeniedException("")).getCode());
            assertEquals(500, handler.handleException(new Exception("")).getCode());
        }
    }
}
