package com.zlf.fufuzhibo.server.service;

import com.zlf.fufuzhibo.pojo.vo.AdminLiveRoomVO;
import com.zlf.fufuzhibo.pojo.vo.PageResult;

import java.time.LocalDateTime;

/**
 * 管理端直播房间业务
 */
public interface AdminLiveRoomService {

    /**
     * 分页查询全部直播间
     *
     * @param page    页码，从 1 开始
     * @param size    每页条数
     * @param status  直播间状态，为空不过滤
     * @param keyword 主播昵称或流名关键词，为空不过滤
     */
    PageResult<AdminLiveRoomVO> listRooms(int page, int size, Integer status, String keyword);

    /**
     * 封禁指定主播的直播间，封禁前先结束正在进行的直播
     *
     * @param userId  主播用户 id
     * @param banTime 封禁截止时间
     */
    void ban(Long userId, LocalDateTime banTime);

    /**
     * 解封指定主播的直播间
     *
     * @param userId 主播用户 id
     */
    void unban(Long userId);

    /**
     * 将已到封禁截止时间的直播间恢复为未开播
     *
     * @return 恢复的直播间数量
     */
    int unbanExpired();
}
