package com.zlf.fufuzhibo.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlf.fufuzhibo.pojo.entity.LiveMessage;
import com.zlf.fufuzhibo.pojo.vo.ChatMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LiveMessageMapper extends BaseMapper<LiveMessage> {

    /**
     * 按场次内的 seq 游标向前翻页查询正常消息，结果按 seq 倒序
     *
     * @param sessionId 直播场次 id，消息按场次隔离
     * @param lastSeq   游标，仅返回 seq 小于该值的消息，为空时返回最新的一页
     * @param size      条数
     */
    List<ChatMessageVO> selectHistory(@Param("sessionId") Long sessionId,
                                      @Param("lastSeq") Long lastSeq,
                                      @Param("size") int size);

    /**
     * 批量写入聊天消息，依赖唯一索引 (session_id, seq) 做幂等
     *
     * @param messages 待写入消息，调用方保证非空
     * @return 实际写入行数
     */
    int insertBatch(@Param("list") List<LiveMessage> messages);

    /**
     * 查询场次已落库的最大消息序号，Redis 序号键丢失后用于恢复计数基准
     *
     * @param sessionId 直播场次 id
     */
    @Select("SELECT MAX(seq) FROM live_message WHERE session_id = #{sessionId}")
    Long selectMaxSeq(@Param("sessionId") Long sessionId);
}
