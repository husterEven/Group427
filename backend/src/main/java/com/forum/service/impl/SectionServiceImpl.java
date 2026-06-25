package com.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.forum.entity.Section;
import com.forum.entity.Zone;
import com.forum.mapper.SectionMapper;
import com.forum.mapper.ZoneMapper;
import com.forum.service.SectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SectionServiceImpl implements SectionService {

    private final SectionMapper sectionMapper;
    private final ZoneMapper zoneMapper;

    @Override
    public List<Section> getAllSections() {
        QueryWrapper<Section> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort_order");
        List<Section> sections = sectionMapper.selectList(wrapper);
        for (Section section : sections) {
            List<Zone> zones = zoneMapper.selectBySectionId(section.getSectionId());
            section.setZones(zones);
        }
        return sections;
    }

    @Override
    public List<Zone> getZones(Integer sectionId) {
        return zoneMapper.selectBySectionId(sectionId);
    }
}
