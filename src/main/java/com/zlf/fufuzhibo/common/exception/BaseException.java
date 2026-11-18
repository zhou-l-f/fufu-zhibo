package com.zlf.fufuzhibo.common.exception;

import com.zlf.fufuzhibo.common.constant.ErrorCode;

public class BaseException extends RuntimeException {

    /**
     * 业务错误码，为空时按历史逻辑仅返回错误信息
     */
    private final Integer code;

    public BaseException() {
        this.code = null;
    }

    public BaseException(String message) {
        super(message);
        this.code = null;
    }

    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public Integer getCode() {
        return code;
    }
}
