package com.zlf.fufuzhibo.server.service;

import com.zlf.fufuzhibo.pojo.dto.StartLiveDTO;
import com.zlf.fufuzhibo.pojo.vo.LiveRoomVO;
import com.zlf.fufuzhibo.pojo.vo.StartLiveVO;

/**
 * 直播间与开播申请
 */
public interface LiveRoomService {

    /**
     * 注册直播间：校验开播资格后分配唯一流名，重复调用返回已有直播间
     *
     * @param userId 当前登录用户 id
     * @return 直播间信息
     */
    LiveRoomVO registerRoom(Long userId);

    /**
     * 申请开始直播：创建待开播场次并签发短效推流 token
     *
     * @param userId 当前登录用户 id
     * @param dto    开播信息，包含标题与可选封面
     * @return 推流地址与播放地址
     */
    StartLiveVO startLive(Long userId, StartLiveDTO dto);
}
