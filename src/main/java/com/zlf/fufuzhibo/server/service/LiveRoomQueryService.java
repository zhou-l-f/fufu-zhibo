package com.zlf.fufuzhibo.server.service;

import com.zlf.fufuzhibo.pojo.vo.LiveRoomDetailVO;
import com.zlf.fufuzhibo.pojo.vo.LiveRoomItemVO;
import com.zlf.fufuzhibo.pojo.vo.PageResult;

/**
 * 直播间浏览（匿名）：首页房间列表与直播间详情
 */
public interface LiveRoomQueryService {

    /**
     * 分页查询正在直播的直播间
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 分页结果
     */
    PageResult<LiveRoomItemVO> listLiveRooms(int page, int size);

    /**
     * 查询直播间详情，不存在或已下播时抛出 LIVE_ROOM_NOT_FOUND
     *
     * @param roomId 直播间 id
     * @return 直播间详情
     */
    LiveRoomDetailVO getLiveRoom(Long roomId);
}
