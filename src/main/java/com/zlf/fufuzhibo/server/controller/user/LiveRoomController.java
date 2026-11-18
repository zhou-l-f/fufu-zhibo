package com.zlf.fufuzhibo.server.controller.user;

import com.zlf.fufuzhibo.common.constant.MessageConstant;
import com.zlf.fufuzhibo.common.exception.BaseException;
import com.zlf.fufuzhibo.common.result.Result;
import com.zlf.fufuzhibo.pojo.dto.StartLiveDTO;
import com.zlf.fufuzhibo.pojo.vo.LiveRoomVO;
import com.zlf.fufuzhibo.pojo.vo.StartLiveVO;
import com.zlf.fufuzhibo.server.service.LiveRoomService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "直播管理接口")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/live")
public class LiveRoomController {

    private final LiveRoomService liveRoomService;

    /**
     * 注册直播间：要求已完成实名认证，重复调用返回已有直播间
     */
    @PostMapping("/room/register")
    public Result<LiveRoomVO> registerRoom() {
        Long userId = currentUserId();
        log.info("注册直播间: userId={}", userId);
        return Result.success(liveRoomService.registerRoom(userId));
    }

    /**
     * 申请开始直播：创建待开播场次，返回推流地址与短效 token
     */
    @PostMapping("/start")
    public Result<StartLiveVO> startLive(@RequestBody @Valid StartLiveDTO dto) {
        Long userId = currentUserId();
        log.info("申请开始直播: userId={}, title={}", userId, dto.getTitle());
        return Result.success(liveRoomService.startLive(userId, dto));
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal == null) {
            throw new BaseException(MessageConstant.USER_NOT_LOGIN);
        }
        return Long.valueOf(principal.toString());
    }
}
