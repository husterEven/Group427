package com.forum.service;

import com.forum.common.PageResult;
import com.forum.dto.DynamicCreateRequest;
import com.forum.entity.RealtimeDynamic;

import java.util.List;

public interface DynamicService {

    PageResult<RealtimeDynamic> getFeed(int page, int pageSize, String filter);

    RealtimeDynamic createDynamic(DynamicCreateRequest req);

    void deleteDynamic(Long dynamicId);

    List<RealtimeDynamic> getByUser(Long userId);
}
