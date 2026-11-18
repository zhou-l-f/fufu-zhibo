package com.zlf.fufuzhibo.server.controller.publicapi;

import com.zlf.fufuzhibo.common.result.Result;
import com.zlf.fufuzhibo.pojo.vo.LiveRoomDetailVO;
import com.zlf.fufuzhibo.pojo.vo.LiveRoomItemVO;
import com.zlf.fufuzhibo.pojo.vo.PageResult;
import com.zlf.fufuzhibo.server.service.LiveRoomQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 匿名可访问的直播间浏览接口，无需登录与实名认证
 */
@Tag(name = "公开直播间接口", description = "无需登录即可访问的直播间浏览接口")
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/public/live-rooms")
public class PublicLiveRoomController {

    private final LiveRoomQueryService liveRoomQueryService;

    @Operation(summary = "分页查询直播间",
            description = "按开播时间倒序分页返回正在直播的直播间，包含标题、封面与在线观看人数；未登录也可访问，页码超范围时返回空列表。")
    @GetMapping
    public Result<PageResult<LiveRoomItemVO>> listLiveRooms(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码不能小于 1") Integer page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 50, message = "每页条数不能超过 50") Integer size) {
        log.info("查询直播间列表: page={}, size={}", page, size);
        return Result.success(liveRoomQueryService.listLiveRooms(page, size));
    }

    @Operation(summary = "查询直播间详情",
            description = "返回直播间标题、封面、在线观看人数与播放地址。HTTP 状态码始终为 200，业务结果通过 code 判断："
                    + "直播间不存在、已下播或已封禁时返回 40401，前端应按「直播已结束」处理。")
    @GetMapping("/{roomId}")
    public Result<LiveRoomDetailVO> getLiveRoom(
            @PathVariable @Positive(message = "直播间 id 不合法") Long roomId) {
        log.info("查询直播间详情: roomId={}", roomId);
        return Result.success(liveRoomQueryService.getLiveRoom(roomId));
    }
}
