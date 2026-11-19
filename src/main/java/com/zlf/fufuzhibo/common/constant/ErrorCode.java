package com.zlf.fufuzhibo.common.constant;

/**
 * 统一业务错误码
 */
public enum ErrorCode {

    /**
     * 请求参数不合法：页码、分页大小、路径参数类型错误等
     */
    PARAM_INVALID(40001, "请求参数不合法"),

    /**
     * 未登录或登录态已过期
     */
    UNAUTHORIZED(40100, "未登录或登录已过期"),

    /**
     * 已登录但无权访问该资源
     */
    FORBIDDEN(40300, "无权访问该资源"),

    /**
     * 直播间不存在或已下播
     */
    LIVE_ROOM_NOT_FOUND(40401, "直播间不存在或已下播"),

    /**
     * 系统内部异常（数据库、缓存等），不向前端暴露细节
     */
    SYSTEM_ERROR(50000, "系统繁忙，请稍后重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
