package com.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.entity.PostCollect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostCollectMapper extends BaseMapper<PostCollect> {

    @Select("SELECT * FROM post_collect WHERE user_id = #{userId} AND post_id = #{postId}")
    PostCollect selectByUserAndPost(@Param("userId") Long userId, @Param("postId") Long postId);
}
