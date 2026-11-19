package com.zlf.fufuzhibo.pojo.vo;

import lombok.Data;

/**
 * 聊天发言校验结果：一次 JOIN 查出场次、直播间与用户状态，避免每条消息多次查库
 */
@Data
public class ChatContextVO {

    /**
     * 场次状态
     */
    private Integer sessionStatus;

    /**
     * 场次所属直播间 id，用于校验 roomId 是否匹配
     */
    private Long sessionRoomId;

    /**
     * 直播间 id，直播间不存在时为空
     */
    private Long roomId;

    /**
     * 直播间状态
     */
    private Integer roomStatus;

    /**
     * 用户 id，用户不存在时为空
     */
    private Long userId;

    /**
     * 用户状态
     */
    private Integer userStatus;

    private String userName;

    private String avatarUrl;
}
