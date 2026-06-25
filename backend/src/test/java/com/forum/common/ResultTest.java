package com.forum.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Result 响应封装 单元测试")
class ResultTest {

    @Nested
    @DisplayName("ok() 成功响应")
    class OkMethod {

        @Test
        @DisplayName("ok(data) 应返回 code=200, message='success'")
        void okWithData_shouldSetDefaultFields() {
            Result<String> result = Result.ok("hello");
            assertEquals(200, result.getCode());
            assertEquals("success", result.getMessage());
            assertEquals("hello", result.getData());
        }

        @Test
        @DisplayName("ok(data) 应支持 null data")
        void okWithNullData_shouldWork() {
            Result<Object> result = Result.ok(null);
            assertEquals(200, result.getCode());
            assertEquals("success", result.getMessage());
            assertNull(result.getData());
        }

        @Test
        @DisplayName("ok(message, data) 应使用自定义 message")
        void okWithMessage_shouldUseCustomMessage() {
            Result<Integer> result = Result.ok("操作成功", 42);
            assertEquals(200, result.getCode());
            assertEquals("操作成功", result.getMessage());
            assertEquals(42, result.getData());
        }

        @Test
        @DisplayName("ok() 不同类型应保持类型安全")
        void ok_shouldPreserveGenericType() {
            Result<Long> longResult = Result.ok(1L);
            Result<String> strResult = Result.ok("text");
            assertEquals(Long.class, longResult.getData().getClass());
            assertEquals(String.class, strResult.getData().getClass());
        }
    }

    @Nested
    @DisplayName("fail() 失败响应")
    class FailMethod {

        @Test
        @DisplayName("fail 应设置自定义 code 和 message")
        void fail_shouldSetCodeAndMessage() {
            Result<String> result = Result.fail(404, "资源未找到");
            assertEquals(404, result.getCode());
            assertEquals("资源未找到", result.getMessage());
            assertNull(result.getData());
        }

        @Test
        @DisplayName("fail 应支持 500 错误码")
        void failWith500_shouldWork() {
            Result<Void> result = Result.fail(500, "服务器内部错误");
            assertEquals(500, result.getCode());
            assertEquals("服务器内部错误", result.getMessage());
        }

        @Test
        @DisplayName("fail 应支持 401 未授权")
        void failWith401_shouldWork() {
            Result<Void> result = Result.fail(401, "未登录");
            assertEquals(401, result.getCode());
        }
    }

    @Nested
    @DisplayName("error() 快捷错误")
    class ErrorMethod {

        @Test
        @DisplayName("error 应使用默认 code=400")
        void error_shouldUseDefaultCode() {
            Result<String> result = Result.error("参数错误");
            assertEquals(400, result.getCode());
            assertEquals("参数错误", result.getMessage());
            assertNull(result.getData());
        }

        @Test
        @DisplayName("error 等同于 fail(400, message)")
        void error_shouldBeEquivalentToFail400() {
            Result<String> errorResult = Result.error("test");
            Result<String> failResult = Result.fail(400, "test");
            assertEquals(failResult.getCode(), errorResult.getCode());
            assertEquals(failResult.getMessage(), errorResult.getMessage());
        }
    }

    @Nested
    @DisplayName("Lombok @Data")
    class DataAnnotation {

        @Test
        @DisplayName("setter/getter 应正常工作")
        void settersAndGetters_shouldWork() {
            Result<String> result = new Result<>();
            result.setCode(200);
            result.setMessage("test");
            result.setData("data");
            assertEquals(200, result.getCode());
            assertEquals("test", result.getMessage());
            assertEquals("data", result.getData());
        }
    }
}
