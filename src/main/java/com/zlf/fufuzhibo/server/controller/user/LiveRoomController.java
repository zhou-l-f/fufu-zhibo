package com.zlf.fufuzhibo.server.controller.user;

import com.zlf.fufuzhibo.common.result.Result;
import com.zlf.fufuzhibo.common.utils.SecurityUtil;
import com.zlf.fufuzhibo.pojo.dto.StartLiveDTO;
import com.zlf.fufuzhibo.pojo.vo.LiveRoomVO;
import com.zlf.fufuzhibo.pojo.vo.StartLiveVO;
import com.zlf.fufuzhibo.server.service.LiveRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "直播管理接口")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class LiveRoomController {

    private final LiveRoomService liveRoomService;

    /**
     * 注册直播间：要求已完成实名认证，重复调用返回已有直播间
     */
    @Operation(summary = "注册直播间",
            description = "为当前登录用户创建直播间并分配唯一流名，要求账号状态正常且已完成实名认证；重复调用直接返回已有直播间。")
    @PostMapping("/live-rooms")
    public Result<LiveRoomVO> registerRoom() {
        Long userId = SecurityUtil.currentUserId();
        log.info("注册直播间: userId={}", userId);
        return Result.success(liveRoomService.registerRoom(userId));
    }

    /**
     * 申请开始直播：创建待开播场次，上传封面并返回推流地址与短效 token
     */
    @Operation(summary = "申请开始直播",
            description = "创建待开播场次并签发短效推流 token，返回 RTMP 推流地址与 HTTP-FLV/HLS 播放地址；封面图片可选，上传成功后写入本场直播记录。")
    @PostMapping(value = "/live-sessions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<StartLiveVO> startLive(@ModelAttribute @Valid StartLiveDTO dto) {
        Long userId = SecurityUtil.currentUserId();
        log.info("申请开始直播: userId={}, title={}", userId, dto.getTitle());
        return Result.success(liveRoomService.startLive(userId, dto));
    }
}
