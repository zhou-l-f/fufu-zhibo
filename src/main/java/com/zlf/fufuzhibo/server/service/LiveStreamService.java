package com.zlf.fufuzhibo.server.service;

import com.zlf.fufuzhibo.pojo.dto.SrsCallbackDTO;

import java.util.Set;

/**
 * 直播推流业务：处理 SRS 的 on_publish / on_unpublish 回调，并对账僵尸场次
 */
public interface LiveStreamService {

    /**
     * 推流开始：校验 token 与场次，首次推流绑定 clientId，断流宽限期内允许重连恢复
     *
     * @param callback SRS 回调参数
     */
    void onPublish(SrsCallbackDTO callback);

    /**
     * 推流结束：结束会话并恢复直播间状态，重复回调直接忽略
     *
     * @param callback SRS 回调参数
     */
    void onUnpublish(SrsCallbackDTO callback);

    /**
     * 与 SRS 的在线流列表对账，回收回调丢失导致的僵尸场次
     *
     * @param activeStreams SRS 当前在线流集合，元素格式：app/streamName
     * @return 回收的场次数量
     */
    int reconcile(Set<String> activeStreams);
}
