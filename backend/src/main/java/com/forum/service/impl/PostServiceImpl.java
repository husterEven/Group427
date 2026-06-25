package com.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forum.common.PageResult;
import com.forum.common.SecurityUtil;
import com.forum.dto.EssenceRequest;
import com.forum.dto.PinRequest;
import com.forum.dto.PostCreateRequest;
import com.forum.dto.PostQueryRequest;
import com.forum.dto.PostUpdateRequest;
import com.forum.entity.*;
import com.forum.mapper.*;
import com.forum.service.PostService;
import com.forum.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final PostCollectMapper postCollectMapper;
    private final PostLikeMapper postLikeMapper;
    private final UserAchievementMapper userAchievementMapper;
    private final SectionMapper sectionMapper;
    private final ZoneMapper zoneMapper;
    private final SecurityUtil securityUtil;
    private final VoteService voteService;
    private final NotificationHelper notificationHelper;

    @Override
    public PageResult<Post> getPostList(PostQueryRequest req) {
        Page<Post> page = new Page<>(req.getPage(), req.getPageSize());
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.eq("audit_status", 1);
        if (req.getAuthorId() != null) {
            wrapper.eq("author_id", req.getAuthorId());
        }
        if (req.getSectionId() != null) {
            wrapper.eq("section_id", req.getSectionId());
        }
        if (req.getZoneId() != null) {
            wrapper.eq("zone_id", req.getZoneId());
        }
        if (req.getKeyword() != null && !req.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like("title", req.getKeyword()).or().like("content", req.getKeyword()));
        }
        if (req.getIsEssence() != null && req.getIsEssence()) {
            wrapper.eq("is_essence", 1);
        }
        String sort = req.getSort();
        if ("hot".equals(sort)) {
            wrapper.orderByDesc("(like_count + comment_count)");
        } else if ("popular".equals(sort)) {
            wrapper.orderByDesc("like_count");
        } else {
            wrapper.orderByDesc("publish_time");
        }
        Page<Post> result = postMapper.selectPage(page, wrapper);
        enrichPosts(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(),
                (int) result.getCurrent(), (int) result.getSize());
    }

    private void enrichPosts(List<Post> posts) {
        if (posts.isEmpty()) return;
        Long currentUserId = null;
        try {
            currentUserId = securityUtil.getCurrentUserId();
        } catch (Exception ignored) {}

        Set<Long> authorIds = posts.stream().map(Post::getAuthorId).collect(Collectors.toSet());
        Set<Integer> sectionIds = posts.stream().map(Post::getSectionId).collect(Collectors.toSet());
        Set<Integer> zoneIds = posts.stream().map(Post::getZoneId).filter(id -> id != null).collect(Collectors.toSet());
        Set<Long> postIds = posts.stream().map(Post::getPostId).collect(Collectors.toSet());

        Map<Long, User> userMap = userMapper.selectBatchIds(authorIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));
        Map<Integer, String> sectionMap = sectionMapper.selectBatchIds(sectionIds).stream()
                .collect(Collectors.toMap(s -> s.getSectionId(), s -> s.getSectionName()));
        Map<Integer, String> zoneMap = zoneIds.isEmpty() ? Map.of() :
                zoneMapper.selectBatchIds(zoneIds).stream()
                        .collect(Collectors.toMap(z -> z.getZoneId(), z -> z.getZoneName()));
        Map<Long, Boolean> likedMap = Map.of();
        Map<Long, Boolean> collectedMap = Map.of();
        if (currentUserId != null) {
            QueryWrapper<PostCollect> collectWrapper = new QueryWrapper<>();
            collectWrapper.in("post_id", postIds).eq("user_id", currentUserId);
            collectedMap = postCollectMapper.selectList(collectWrapper).stream()
                    .collect(Collectors.toMap(PostCollect::getPostId, c -> true));

            QueryWrapper<PostLike> likeWrapper = new QueryWrapper<>();
            likeWrapper.in("post_id", postIds).eq("user_id", currentUserId);
            likedMap = postLikeMapper.selectList(likeWrapper).stream()
                    .collect(Collectors.toMap(PostLike::getPostId, l -> true));
        }

        for (Post post : posts) {
            User user = userMap.get(post.getAuthorId());
            if (user != null) {
                User brief = new User();
                brief.setUserId(user.getUserId());
                brief.setNickname(user.getNickname());
                brief.setAvatarUrl(user.getAvatarUrl());
                brief.setLevel(user.getLevel());
                brief.setVerificationLevel(user.getVerificationLevel());
                post.setAuthor(brief);
            }
            post.setSectionName(sectionMap.get(post.getSectionId()));
            if (post.getZoneId() != null) {
                post.setZoneName(zoneMap.get(post.getZoneId()));
            }
            post.setIsLiked(likedMap.containsKey(post.getPostId()));
            post.setIsCollected(collectedMap.containsKey(post.getPostId()));
        }
    }

    @Override
    public Post getPostDetail(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        post.setViewCount(post.getViewCount() + 1);
        postMapper.updateById(post);
        enrichPosts(List.of(post));

        if (post.getContentType() != null && post.getContentType() == 1) {
            try {
                post.setVote(voteService.getVoteByPost(postId));
            } catch (Exception ignored) {}
        }
        return post;
    }

    @Override
    public Post createPost(PostCreateRequest req) {
        Long userId = securityUtil.getCurrentUserId();
        Post post = new Post();
        post.setAuthorId(userId);
        post.setTitle(req.getTitle());
        post.setContent(req.getContent());
        post.setContentType(req.getContentType());
        post.setSectionId(req.getSectionId());
        post.setZoneId(req.getZoneId());
        post.setAuditStatus(1);
        post.setLikeCount(0);
        post.setViewCount(0);
        post.setCommentCount(0);
        post.setCollectCount(0);
        post.setIsEssence(0);
        post.setIsPinned(0);
        post.setPublishTime(LocalDateTime.now());
        postMapper.insert(post);
        QueryWrapper<UserAchievement> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        UserAchievement achievement = userAchievementMapper.selectOne(wrapper);
        if (achievement == null) {
            achievement = new UserAchievement();
            achievement.setUserId(userId);
            achievement.setTotalPostCount(1);
            achievement.setEssencePostCount(0);
            userAchievementMapper.insert(achievement);
        } else {
            achievement.setTotalPostCount(achievement.getTotalPostCount() + 1);
            userAchievementMapper.updateById(achievement);
        }
        return post;
    }

    @Override
    public Post updatePost(Long postId, PostUpdateRequest req) {
        Long userId = securityUtil.getCurrentUserId();
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        if (!post.getAuthorId().equals(userId)) {
            throw new RuntimeException("无权修改他人帖子");
        }
        if (req.getTitle() != null) {
            post.setTitle(req.getTitle());
        }
        if (req.getContent() != null) {
            post.setContent(req.getContent());
        }
        if (req.getSectionId() != null) {
            post.setSectionId(req.getSectionId());
        }
        if (req.getZoneId() != null) {
            post.setZoneId(req.getZoneId());
        }
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
        return post;
    }

    @Override
    public void deletePost(Long postId) {
        Long userId = securityUtil.getCurrentUserId();
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        if (!post.getAuthorId().equals(userId)) {
            throw new RuntimeException("无权删除他人帖子");
        }
        post.setIsDeleted(1);
        postMapper.updateById(post);
    }

    @Override
    public Map<String, Object> toggleLike(Long postId) {
        Long userId = securityUtil.getCurrentUserId();
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        PostLike existing = postLikeMapper.selectByUserAndPost(userId, postId);
        Map<String, Object> result = new HashMap<>();
        if (existing != null) {
            postLikeMapper.deleteById(existing.getLikeId());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postMapper.updateById(post);
            result.put("isLiked", false);
        } else {
            PostLike like = new PostLike();
            like.setUserId(userId);
            like.setPostId(postId);
            like.setCreatedAt(LocalDateTime.now());
            postLikeMapper.insert(like);
            post.setLikeCount(post.getLikeCount() + 1);
            postMapper.updateById(post);
            result.put("isLiked", true);
            if (!userId.equals(post.getAuthorId())) {
                User currentUser = userMapper.selectById(userId);
                String likerName = currentUser != null ? currentUser.getNickname() : "用户";
                notificationHelper.createNotification(
                    post.getAuthorId(), 1, "帖子被点赞",
                    likerName + " 点赞了你的帖子", 0, postId
                );
            }
        }
        result.put("likeCount", post.getLikeCount());
        return result;
    }

    @Override
    public Map<String, Object> toggleCollect(Long postId) {
        Long userId = securityUtil.getCurrentUserId();
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        PostCollect existing = postCollectMapper.selectByUserAndPost(userId, postId);
        Map<String, Object> result = new HashMap<>();
        if (existing != null) {
            postCollectMapper.deleteById(existing.getCollectId());
            post.setCollectCount(Math.max(0, post.getCollectCount() - 1));
            postMapper.updateById(post);
            result.put("isCollected", false);
        } else {
            PostCollect collect = new PostCollect();
            collect.setUserId(userId);
            collect.setPostId(postId);
            collect.setCreatedAt(LocalDateTime.now());
            postCollectMapper.insert(collect);
            post.setCollectCount(post.getCollectCount() + 1);
            postMapper.updateById(post);
            result.put("isCollected", true);
        }
        result.put("collectCount", post.getCollectCount());
        return result;
    }

    @Override
    public PageResult<Post> getCollections(int page, int pageSize) {
        Long userId = securityUtil.getCurrentUserId();
        QueryWrapper<PostCollect> collectWrapper = new QueryWrapper<>();
        collectWrapper.eq("user_id", userId);
        collectWrapper.orderByDesc("created_at");
        Page<PostCollect> collectPage = new Page<>(page, pageSize);
        Page<PostCollect> collectResult = postCollectMapper.selectPage(collectPage, collectWrapper);
        List<Long> postIds = collectResult.getRecords().stream()
                .map(PostCollect::getPostId)
                .collect(Collectors.toList());
        List<Post> posts = postIds.isEmpty() ? List.of() : postMapper.selectBatchIds(postIds);
        enrichPosts(posts);
        return PageResult.of(posts, collectResult.getTotal(), page, pageSize);
    }

    @Override
    public void togglePin(Long postId, PinRequest req) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        post.setIsPinned(req.getIsPinned() ? 1 : 0);
        postMapper.updateById(post);
    }

    @Override
    public void toggleEssence(Long postId, EssenceRequest req) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        post.setIsEssence(req.getIsEssence() ? 1 : 0);
        postMapper.updateById(post);
    }
}
