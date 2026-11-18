package com.zlf.fufuzhibo.server.controller.admin;

import com.zlf.fufuzhibo.common.result.Result;
import com.zlf.fufuzhibo.pojo.dto.AdminBanRoomDTO;
import com.zlf.fufuzhibo.pojo.vo.AdminLiveRoomVO;
import com.zlf.fufuzhibo.pojo.vo.PageResult;
import com.zlf.fufuzhibo.server.service.AdminLiveRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端直播间管理接口
 */
@Tag(name = "管理端直播房间接口")
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/live-rooms")
public class AdminLiveRoomController {

    private final AdminLiveRoomService adminLiveRoomService;

    @Operation(summary = "分页查询全部直播间",
            description = "按创建时间倒序返回所有直播间，可按状态（0 未开播、1 直播中、2 封禁）"
                    + "和主播昵称/流名关键词筛选。")
    @GetMapping
    public Result<PageResult<AdminLiveRoomVO>> listRooms(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码不能小于 1") Integer page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 50, message = "每页条数不能超过 50") Integer size,
            @RequestParam(required = false)
            @Min(value = 0, message = "状态取值不正确")
            @Max(value = 2, message = "状态取值不正确") Integer status,
            @RequestParam(required = false) String keyword) {
        log.info("管理端查询直播间列表: page={}, size={}, status={}, keyword={}",
                page, size, status, keyword);
        return Result.success(adminLiveRoomService.listRooms(page, size, status, keyword));
    }

    @Operation(summary = "封禁直播间",
            description = "按主播用户 id 封禁直播间；若正在直播，会先结束当前场次并断开推流。"
                    + "封禁截止时间到达后由定时任务自动解封。")
    @PostMapping("/{userId}/ban")
    public Result<Void> ban(@PathVariable Long userId,
                            @RequestBody @Valid AdminBanRoomDTO banRoomDTO) {
        log.info("管理端封禁直播间: userId={}, banTime={}", userId, banRoomDTO.getBanTime());
        adminLiveRoomService.ban(userId, banRoomDTO.getBanTime());
        return Result.success();
    }

    @Operation(summary = "解封直播间",
            description = "按主播用户 id 解封直播间，解封后状态恢复为未开播。")
    @PostMapping("/{userId}/unban")
    public Result<Void> unban(@PathVariable Long userId) {
        log.info("管理端解封直播间: userId={}", userId);
        adminLiveRoomService.unban(userId);
        return Result.success();
    }
}
