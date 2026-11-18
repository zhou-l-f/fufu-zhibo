package com.zlf.fufuzhibo.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlf.fufuzhibo.pojo.entity.LiveRoom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LiveRoomMapper extends BaseMapper<LiveRoom> {

    /**
     * 加行锁查询用户的直播间，用于串行化“申请开播”，避免并发创建多个待开播场次
     */
    @Select("SELECT * FROM live_room WHERE user_id = #{userId} FOR UPDATE")
    LiveRoom getByUserIdForUpdate(@Param("userId") Long userId);
}
