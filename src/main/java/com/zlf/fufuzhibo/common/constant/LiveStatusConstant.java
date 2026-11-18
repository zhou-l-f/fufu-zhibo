package com.zlf.fufuzhibo.common.constant;

/**
 * 直播状态常量
 */
public class LiveStatusConstant {

    /** 直播间：未开播 */
    public static final Integer ROOM_NOT_STARTED = 0;
    /** 直播间：直播中 */
    public static final Integer ROOM_LIVE = 1;
    /** 直播间：封禁 */
    public static final Integer ROOM_BANNED = 2;

    /** 推流会话：待开播 */
    public static final Integer SESSION_PENDING = 0;
    /** 推流会话：直播中 */
    public static final Integer SESSION_LIVE = 1;
    /** 推流会话：已结束 */
    public static final Integer SESSION_ENDED = 2;

    /** 结束原因：正常断流 */
    public static final Integer END_REASON_NORMAL = 1;
    /** 结束原因：超时回收 */
    public static final Integer END_REASON_TIMEOUT = 2;
    /** 结束原因：异常 */
    public static final Integer END_REASON_ERROR = 3;
    /** 结束原因：未开播即被新场次替代 */
    public static final Integer END_REASON_CANCELLED = 4;
}
