package com.zlf.fufuzhibo.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlf.fufuzhibo.pojo.entity.LiveMessage;
import com.zlf.fufuzhibo.pojo.vo.ChatMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LiveMessageMapper extends BaseMapper<LiveMessage> {

    /**
     * 按消息 id 游标向前翻页查询正常消息，结果按 id 倒序
     *
     * @param roomId 直播间 id
     * @param lastId 游标，仅返回 id 小于该值的消息，为空时返回最新的一页
     * @param size   条数
     */
    List<ChatMessageVO> selectHistory(@Param("roomId") Long roomId,
                                      @Param("lastId") Long lastId,
                                      @Param("size") int size);
}
