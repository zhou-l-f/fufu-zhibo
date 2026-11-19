package com.zlf.fufuzhibo.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlf.fufuzhibo.pojo.entity.LiveSession;
import com.zlf.fufuzhibo.pojo.vo.ChatContextVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LiveSessionMapper extends BaseMapper<LiveSession> {

    /**
     * 一次查询获取发言校验所需的场次、直播间与用户状态，
     * 用 LEFT JOIN 保留「直播间/用户不存在」的区分能力
     */
    @Select("""
            SELECT s.status AS sessionStatus,
                   s.room_id AS sessionRoomId,
                   r.id AS roomId,
                   r.status AS roomStatus,
                   u.id AS userId,
                   u.status AS userStatus,
                   u.`name` AS userName,
                   u.avatar_url AS avatarUrl
            FROM live_session s
            LEFT JOIN live_room r ON r.id = s.room_id
            LEFT JOIN `user` u ON u.id = #{userId}
            WHERE s.id = #{sessionId}
            """)
    ChatContextVO selectChatContext(@Param("sessionId") Long sessionId,
                                    @Param("userId") Long userId);
}
