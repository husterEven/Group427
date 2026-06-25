package com.forum.service;

import com.forum.entity.Section;
import com.forum.entity.Zone;

import java.util.List;

public interface SectionService {

    List<Section> getAllSections();

    List<Zone> getZones(Integer sectionId);
}
