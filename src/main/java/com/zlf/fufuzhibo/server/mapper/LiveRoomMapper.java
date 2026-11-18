package com.zlf.fufuzhibo.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zlf.fufuzhibo.pojo.entity.LiveRoom;
import com.zlf.fufuzhibo.pojo.vo.LiveRoomDetailVO;
import com.zlf.fufuzhibo.pojo.vo.LiveRoomItemVO;
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

    /**
     * 分页查询正在直播的直播间，由 MyBatis-Plus 分页插件自动拼接 LIMIT 与 COUNT
     *
     * @param page 分页参数
     */
    IPage<LiveRoomItemVO> selectLiveRoomPage(IPage<LiveRoomItemVO> page);

    /**
     * 查询直播间详情，仅返回正在直播的直播间，不存在时返回 null
     */
    LiveRoomDetailVO selectLiveRoomDetail(@Param("roomId") Long roomId);
}
