package com.zlf.fufuzhibo.server.controller.user;

import com.zlf.fufuzhibo.common.result.Result;
import com.zlf.fufuzhibo.common.utils.SecurityUtil;
import com.zlf.fufuzhibo.pojo.dto.StartLiveDTO;
import com.zlf.fufuzhibo.pojo.vo.LiveHistoryItemVO;
import com.zlf.fufuzhibo.pojo.vo.LiveRoomVO;
import com.zlf.fufuzhibo.pojo.vo.MyLiveRoomVO;
import com.zlf.fufuzhibo.pojo.vo.PageResult;
import com.zlf.fufuzhibo.pojo.vo.StartLiveVO;
import com.zlf.fufuzhibo.server.service.LiveRoomService;
import com.zlf.fufuzhibo.server.service.LiveStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "直播管理接口")
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class LiveRoomController {

    private final LiveRoomService liveRoomService;
    private final LiveStreamService liveStreamService;

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
     * 查询我的直播间：用于主播端刷新后恢复开播状态
     */
    @Operation(summary = "查询我的直播间",
            description = "返回当前用户的直播间、直播间状态以及未结束的场次信息；未创建直播间时 hasRoom=false，可用于主播中心初始化。")
    @GetMapping("/live-rooms/me")
    public Result<MyLiveRoomVO> getMyRoom() {
        Long userId = SecurityUtil.currentUserId();
        log.info("查询我的直播间: userId={}", userId);
        return Result.success(liveRoomService.getMyRoom(userId));
    }

    /**
     * 查询我的直播记录：主播个人主页使用，包含进行中与历史场次
     */
    @Operation(summary = "分页查询我的直播记录",
            description = "按创建时间倒序返回当前用户的直播场次，包含待开播、直播中与已结束场次；"
                    + "被新场次作废且从未推流的记录不会返回。")
    @GetMapping("/live-sessions/me")
    public Result<PageResult<LiveHistoryItemVO>> listMyLiveSessions(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码不能小于 1") Integer page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 50, message = "每页条数不能超过 50") Integer size) {
        Long userId = SecurityUtil.currentUserId();
        log.info("查询我的直播记录: userId={}, page={}, size={}", userId, page, size);
        return Result.success(liveRoomService.listMyLiveSessions(userId, page, size));
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

    /**
     * 结束当前直播：主播主动下播，同时通知 SRS 断开推流
     */
    @Operation(summary = "结束当前直播",
            description = "结束当前用户进行中的直播场次（含待开播场次），回写直播间状态并尽力通知 SRS 断开推流；重复调用按幂等成功返回。")
    @PostMapping("/live-sessions/current/end")
    public Result<Void> endCurrentLive() {
        Long userId = SecurityUtil.currentUserId();
        log.info("主播主动结束直播: userId={}", userId);
        liveStreamService.endCurrentLive(userId);
        return Result.success();
    }
}
