package com.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    @Select("SELECT * FROM comment_like WHERE user_id = #{userId} AND comment_id = #{commentId}")
    CommentLike selectByUserAndComment(@Param("userId") Long userId, @Param("commentId") Long commentId);
}
