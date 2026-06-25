package com.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.entity.VoteRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface VoteRecordMapper extends BaseMapper<VoteRecord> {

    @Select("SELECT * FROM vote_record WHERE vote_id = #{voteId} AND user_id = #{userId}")
    VoteRecord selectByVoteAndUser(@Param("voteId") Long voteId, @Param("userId") Long userId);

    @Select("SELECT option_index, COUNT(*) as cnt FROM vote_record WHERE vote_id = #{voteId} GROUP BY option_index")
    List<Map<String, Object>> countByVoteId(@Param("voteId") Long voteId);
}
