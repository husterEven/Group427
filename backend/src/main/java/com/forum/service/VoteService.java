package com.forum.service;

import com.forum.dto.VoteCreateRequest;
import com.forum.dto.VoteSubmitRequest;
import com.forum.entity.VotePost;

import java.util.Map;

public interface VoteService {

    VotePost createVote(Long postId, VoteCreateRequest req);

    Map<String, Object> submitVote(Long voteId, VoteSubmitRequest req);

    Map<String, Object> getVoteByPost(Long postId);

    Map<String, Object> getVoteResult(Long voteId);
}
