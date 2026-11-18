package com.zlf.fufuzhibo.server.handler;

import com.zlf.fufuzhibo.common.constant.ErrorCode;
import com.zlf.fufuzhibo.common.constant.MessageConstant;
import com.zlf.fufuzhibo.common.exception.BaseException;
import com.zlf.fufuzhibo.common.result.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常，带业务错误码时一并返回
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        if (ex.getCode() == null) {
            return Result.error(ex.getMessage());
        }
        return Result.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler
    public Result<String> exceptionHandler(MethodArgumentNotValidException ex){
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse(MessageConstant.UNKNOWN_ERROR);
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 方法参数校验失败：@RequestParam / @PathVariable 上的约束
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(ConstraintViolationException ex){
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse(ErrorCode.PARAM_INVALID.getMessage());
        log.warn("参数校验失败：{}", message);
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * Spring MVC 内建方法参数校验失败（未走 AOP 校验时）
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(HandlerMethodValidationException ex){
        String message = ex.getAllErrors().stream()
                .findFirst()
                .map(MessageSourceResolvable::getDefaultMessage)
                .orElse(ErrorCode.PARAM_INVALID.getMessage());
        log.warn("参数校验失败：{}", message);
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 路径参数或查询参数类型不匹配，如 roomId 传入非数字
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(MethodArgumentTypeMismatchException ex){
        String message = "参数 " + ex.getName() + " 类型不正确";
        log.warn("参数类型不匹配：{}", ex.getMessage());
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 缺少必填查询参数
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(MissingServletRequestParameterException ex){
        String message = "缺少必填参数 " + ex.getParameterName();
        log.warn("缺少请求参数：{}", ex.getMessage());
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 请求体无法解析，如 JSON 格式错误
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(HttpMessageNotReadableException ex){
        log.warn("请求体解析失败：{}", ex.getMessage());
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), "请求体格式错误");
    }

    @ExceptionHandler
    public Result<String> exceptionHandler(SQLIntegrityConstraintViolationException ex){
        String message = ex.getMessage();
        if(message.contains("Duplicate entry")) {
            String[] split = message.split(" ");
            String username = split[2];
            String msg = username + MessageConstant.ALREADY_EXISTS;
            return Result.error(msg);
        }
        else{
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }

    @ExceptionHandler
    public Result<String> exceptionHandler(MaxUploadSizeExceededException ex){
        return Result.error(MessageConstant.FILE_TOO_LARGE);
    }

    /**
     * 数据库访问异常：不向前端暴露 SQL 细节
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(DataAccessException ex){
        log.error("数据库访问异常", ex);
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
    }

    /**
     * 兜底异常，避免异常堆栈直接返回给前端
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(Exception ex){
        log.error("未预期的系统异常", ex);
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
    }
}
