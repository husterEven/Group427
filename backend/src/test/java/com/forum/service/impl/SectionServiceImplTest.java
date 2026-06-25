package com.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.forum.entity.Section;
import com.forum.entity.Zone;
import com.forum.mapper.SectionMapper;
import com.forum.mapper.ZoneMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SectionServiceImpl 板块服务 单元测试")
class SectionServiceImplTest {

    @Mock private SectionMapper sectionMapper;
    @Mock private ZoneMapper zoneMapper;

    @InjectMocks
    private SectionServiceImpl sectionService;

    @Nested
    @DisplayName("getAllSections() 获取所有板块")
    class GetAllSections {

        @Test
        @DisplayName("应返回板块列表并填充 zones")
        void shouldReturnSectionsWithZones() {
            Section section = new Section();
            section.setSectionId(1);
            section.setSectionName("A股");

            Zone zone = new Zone();
            zone.setZoneId(1);
            zone.setZoneName("上证指数");

            when(sectionMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(section));
            when(zoneMapper.selectBySectionId(1)).thenReturn(List.of(zone));

            List<Section> result = sectionService.getAllSections();

            assertEquals(1, result.size());
            assertEquals("A股", result.get(0).getSectionName());
            assertEquals(1, result.get(0).getZones().size());
            assertEquals("上证指数", result.get(0).getZones().get(0).getZoneName());
        }

        @Test
        @DisplayName("无板块时应返回空列表")
        void noSections_shouldReturnEmptyList() {
            when(sectionMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

            List<Section> result = sectionService.getAllSections();
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getZones() 获取子分区")
    class GetZones {

        @Test
        @DisplayName("应返回指定板块的子分区列表")
        void shouldReturnZonesForSection() {
            Zone zone = new Zone();
            zone.setZoneId(1);
            zone.setZoneName("深证成指");

            when(zoneMapper.selectBySectionId(1)).thenReturn(List.of(zone));

            List<Zone> result = sectionService.getZones(1);

            assertEquals(1, result.size());
            assertEquals("深证成指", result.get(0).getZoneName());
        }

        @Test
        @DisplayName("无子分区应返回空列表")
        void noZones_shouldReturnEmptyList() {
            when(zoneMapper.selectBySectionId(999)).thenReturn(Collections.emptyList());

            List<Zone> result = sectionService.getZones(999);
            assertTrue(result.isEmpty());
        }
    }
}
