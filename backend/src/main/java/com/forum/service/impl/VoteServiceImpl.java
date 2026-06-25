package com.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.common.SecurityUtil;
import com.forum.dto.VoteCreateRequest;
import com.forum.dto.VoteSubmitRequest;
import com.forum.entity.VotePost;
import com.forum.entity.VoteRecord;
import com.forum.mapper.VotePostMapper;
import com.forum.mapper.VoteRecordMapper;
import com.forum.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteServiceImpl implements VoteService {

    private final VotePostMapper votePostMapper;
    private final VoteRecordMapper voteRecordMapper;
    private final SecurityUtil securityUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public VotePost createVote(Long postId, VoteCreateRequest req) {
        VotePost vote = new VotePost();
        vote.setPostId(postId);
        vote.setVoteTitle(req.getVoteTitle());
        if (req.getEndTime() != null && !req.getEndTime().isEmpty()) {
            vote.setEndTime(LocalDateTime.parse(req.getEndTime()));
        }
        try {
            vote.setOptionsJson(objectMapper.writeValueAsString(req.getOptions()));
        } catch (Exception e) {
            throw new RuntimeException("选项序列化失败");
        }
        votePostMapper.insert(vote);
        return vote;
    }

    @Override
    public Map<String, Object> submitVote(Long voteId, VoteSubmitRequest req) {
        Long userId = securityUtil.getCurrentUserId();
        VoteRecord existing = voteRecordMapper.selectByVoteAndUser(voteId, userId);
        if (existing != null) {
            throw new RuntimeException("您已经投过票了");
        }
        VoteRecord record = new VoteRecord();
        record.setVoteId(voteId);
        record.setUserId(userId);
        record.setOptionIndex(req.getOptionIndex());
        record.setVoteTime(LocalDateTime.now());
        voteRecordMapper.insert(record);
        return getVoteResult(voteId);
    }

    @Override
    public Map<String, Object> getVoteByPost(Long postId) {
        QueryWrapper<VotePost> wrapper = new QueryWrapper<>();
        wrapper.eq("post_id", postId);
        VotePost vote = votePostMapper.selectOne(wrapper);
        if (vote == null) {
            return null;
        }
        return buildVoteDetail(vote);
    }

    @Override
    public Map<String, Object> getVoteResult(Long voteId) {
        VotePost vote = votePostMapper.selectById(voteId);
        if (vote == null) {
            throw new RuntimeException("投票不存在");
        }
        return buildVoteDetail(vote);
    }

    private Map<String, Object> buildVoteDetail(VotePost vote) {
        Map<String, Object> result = new HashMap<>();
        result.put("voteId", vote.getVoteId());
        result.put("voteTitle", vote.getVoteTitle());
        result.put("endTime", vote.getEndTime() != null ? vote.getEndTime().toString() : null);
        result.put("isExpired", vote.getEndTime() != null && vote.getEndTime().isBefore(LocalDateTime.now()));

        List<String> optionTexts = parseOptions(vote.getOptionsJson());
        List<Map<String, Object>> countList = voteRecordMapper.countByVoteId(vote.getVoteId());

        Map<Integer, Integer> countMap = new HashMap<>();
        int totalCount = 0;
        for (Map<String, Object> row : countList) {
            Integer idx = ((Number) row.get("option_index")).intValue();
            Integer cnt = ((Number) row.get("cnt")).intValue();
            countMap.put(idx, cnt);
            totalCount += cnt;
        }
        result.put("totalCount", totalCount);

        Long userId = securityUtil.getCurrentUserIdOrNull();
        VoteRecord myVote = userId != null ? voteRecordMapper.selectByVoteAndUser(vote.getVoteId(), userId) : null;
        Integer mySelection = myVote != null ? myVote.getOptionIndex() : null;
        result.put("mySelection", mySelection);

        List<Map<String, Object>> options = new ArrayList<>();
        for (int i = 0; i < optionTexts.size(); i++) {
            Map<String, Object> opt = new HashMap<>();
            opt.put("index", i);
            opt.put("text", optionTexts.get(i));
            int count = countMap.getOrDefault(i, 0);
            opt.put("count", count);
            double percentage = totalCount > 0 ? (double) count / totalCount * 100 : 0;
            opt.put("percentage", percentage);
            opt.put("isSelected", mySelection != null && mySelection == i);
            options.add(opt);
        }
        result.put("options", options);
        return result;
    }

    private List<String> parseOptions(String json) {
        if (json == null || json.isEmpty()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
