package com.zlf.fufuzhibo.server.service;

import java.util.Map;

/**
 * 直播间在线观看人数统计
 */
public interface ViewerCountService {

    /**
     * 观众开始播放：在线人数 +1
     */
    void onPlay(String streamName);

    /**
     * 观众停止播放：在线人数 -1，最低为 0
     */
    void onStop(String streamName);

    /**
     * 读取在线人数，Redis 不可用时返回 0
     */
    int current(String streamName);

    /**
     * 清空某个直播流的在线人数，场次结束时调用
     */
    void clear(String streamName);

    /**
     * 使用 SRS 在线流数据校正人数，避免回调丢失导致长期偏差
     *
     * @param countByStream streamName -> 在线播放连接数
     */
    void syncCounts(Map<String, Integer> countByStream);
}
