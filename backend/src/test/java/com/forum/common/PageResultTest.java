package com.forum.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PageResult 分页封装 单元测试")
class PageResultTest {

    @Nested
    @DisplayName("of() 工厂方法")
    class OfMethod {

        @Test
        @DisplayName("正常分页数据应正确计算 totalPages")
        void of_shouldCalculateTotalPagesCorrectly() {
            List<String> records = List.of("a", "b", "c");
            PageResult<String> result = PageResult.of(records, 10, 1, 3);
            assertEquals(3, result.getRecords().size());
            assertEquals(10, result.getTotal());
            assertEquals(1, result.getPage());
            assertEquals(3, result.getPageSize());
            assertEquals(4, result.getTotalPages()); // ceil(10/3) = 4
        }

        @Test
        @DisplayName("总数为 0 时 totalPages 应为 0")
        void zeroTotal_shouldReturnZeroPages() {
            PageResult<String> result = PageResult.of(Collections.emptyList(), 0, 1, 10);
            assertEquals(0, result.getTotalPages());
            assertEquals(0, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }

        @Test
        @DisplayName("total 正好整除 pageSize")
        void exactDivision_shouldWork() {
            PageResult<String> result = PageResult.of(List.of("x", "y"), 20, 1, 10);
            assertEquals(2, result.getTotalPages()); // 20/10 = 2
        }

        @Test
        @DisplayName("空列表但 total > 0")
        void emptyRecordsWithTotal_shouldWork() {
            PageResult<String> result = PageResult.of(Collections.emptyList(), 5, 2, 5);
            assertEquals(1, result.getTotalPages());
            assertEquals(2, result.getPage());
            assertTrue(result.getRecords().isEmpty());
        }
    }

    @Nested
    @DisplayName("分页边界值")
    class EdgeCases {

        @ParameterizedTest
        @CsvSource({
                "1, 10, 1",
                "10, 10, 1",
                "11, 10, 2",
                "100, 10, 10",
                "99, 10, 10",
                "101, 10, 11",
        })
        @DisplayName("totalPages = ceil(total / pageSize)")
        void totalPagesCalculation(long total, int pageSize, int expectedPages) {
            PageResult<Object> result = PageResult.of(Collections.emptyList(), total, 1, pageSize);
            assertEquals(expectedPages, result.getTotalPages());
        }
    }
}
