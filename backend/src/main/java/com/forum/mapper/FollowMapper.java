package com.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FollowMapper extends BaseMapper<Follow> {

    @Select("SELECT * FROM follow WHERE follower_id = #{userId}")
    List<Follow> selectByFollowerId(@Param("userId") Long userId);

    @Select("SELECT * FROM follow WHERE followee_id = #{userId}")
    List<Follow> selectByFolloweeId(@Param("userId") Long userId);

    @Select("SELECT * FROM follow WHERE follower_id = #{followerId} AND followee_id = #{followeeId}")
    Follow selectByBoth(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);
}
