package com.forum.controller;

import com.forum.common.GlobalExceptionHandler;
import com.forum.entity.Section;
import com.forum.entity.Zone;
import com.forum.service.SectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SectionController 板块接口 单元测试")
class SectionControllerTest {

    @Mock private SectionService sectionService;
    @InjectMocks private SectionController sectionController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sectionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/sections")
    class GetAllSections {
        @Test
        @DisplayName("应返回板块列表")
        void shouldReturnSections() throws Exception {
            Section s = new Section();
            s.setSectionId(1);
            s.setSectionName("A股");
            when(sectionService.getAllSections()).thenReturn(List.of(s));
            mockMvc.perform(get("/api/v1/sections"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].sectionName").value("A股"));
        }

        @Test
        @DisplayName("无板块时应返回空列表")
        void noSections_shouldReturnEmpty() throws Exception {
            when(sectionService.getAllSections()).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/v1/sections"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/sections/{sectionId}/zones")
    class GetZones {
        @Test
        @DisplayName("应返回分区列表")
        void shouldReturnZones() throws Exception {
            Zone z = new Zone();
            z.setZoneId(1);
            z.setZoneName("上证指数");
            when(sectionService.getZones(1)).thenReturn(List.of(z));
            mockMvc.perform(get("/api/v1/sections/1/zones"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].zoneName").value("上证指数"));
        }
    }
}
