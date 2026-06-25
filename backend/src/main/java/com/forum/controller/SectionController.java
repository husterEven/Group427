package com.forum.controller;

import com.forum.common.Result;
import com.forum.service.SectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;

    @GetMapping
    public Result<?> getAllSections() {
        return Result.ok(sectionService.getAllSections());
    }

    @GetMapping("/{sectionId}/zones")
    public Result<?> getZones(@PathVariable Integer sectionId) {
        return Result.ok(sectionService.getZones(sectionId));
    }
}
