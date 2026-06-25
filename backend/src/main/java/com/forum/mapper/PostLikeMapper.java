package com.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.entity.PostLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {

    @Select("SELECT * FROM post_like WHERE user_id = #{userId} AND post_id = #{postId}")
    PostLike selectByUserAndPost(@Param("userId") Long userId, @Param("postId") Long postId);
}
