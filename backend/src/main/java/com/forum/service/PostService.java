package com.forum.service;

import com.forum.common.PageResult;
import com.forum.dto.EssenceRequest;
import com.forum.dto.PinRequest;
import com.forum.dto.PostCreateRequest;
import com.forum.dto.PostQueryRequest;
import com.forum.dto.PostUpdateRequest;
import com.forum.entity.Post;

import java.util.Map;

public interface PostService {

    PageResult<Post> getPostList(PostQueryRequest req);

    Post getPostDetail(Long postId);

    Post createPost(PostCreateRequest req);

    Post updatePost(Long postId, PostUpdateRequest req);

    void deletePost(Long postId);

    Map<String, Object> toggleLike(Long postId);

    Map<String, Object> toggleCollect(Long postId);

    PageResult<Post> getCollections(int page, int pageSize);

    void togglePin(Long postId, PinRequest req);

    void toggleEssence(Long postId, EssenceRequest req);
}
