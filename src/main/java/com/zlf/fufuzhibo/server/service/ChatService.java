package com.zlf.fufuzhibo.server.service;

import com.zlf.fufuzhibo.pojo.vo.ChatMessageVO;

import java.util.List;

/**
 * 直播间聊天：发言、历史消息与最近消息，全部以直播场次（sessionId）为维度
 */
public interface ChatService {

    /**
     * 校验并保存一条发言，返回可直接广播的消息
     *
     * @param roomId    直播间 id
     * @param sessionId 直播场次 id
     * @param userId    发言用户 id
     * @param content   原始消息内容
     * @return 保存后的消息
     */
    ChatMessageVO sendMessage(Long roomId, Long sessionId, Long userId, String content);

    /**
     * 按场次内 seq 游标分页查询历史消息，结果按时间正序
     *
     * @param roomId    直播间 id，用于校验场次归属
     * @param sessionId 直播场次 id
     * @param lastSeq   游标，仅返回 seq 小于该值的消息，为空时返回最新一页
     * @param size      条数，1~50
     */
    List<ChatMessageVO> listHistory(Long roomId, Long sessionId, Long lastSeq, int size);

    /**
     * 查询某场次的最近消息，优先读 Redis，未命中时回源数据库，结果按时间正序
     *
     * @param sessionId 直播场次 id
     * @param size      条数
     */
    List<ChatMessageVO> listRecent(Long sessionId, int size);
}
