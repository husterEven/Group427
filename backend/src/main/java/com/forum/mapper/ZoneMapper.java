package com.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.entity.Zone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ZoneMapper extends BaseMapper<Zone> {

    @Select("SELECT * FROM zone WHERE section_id = #{sectionId} ORDER BY sort_order")
    List<Zone> selectBySectionId(@Param("sectionId") Integer sectionId);
}
