package com.zlf.fufuzhibo.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端封禁直播间参数
 */
@Data
@Schema(name = "AdminBanRoomDTO", description = "封禁直播间参数")
public class AdminBanRoomDTO {

    @Schema(description = "封禁截止时间，格式 yyyy-MM-ddTHH:mm:ss，必须晚于当前时间",
            example = "2026-11-20T00:00:00")
    @NotNull(message = "封禁截止时间不能为空")
    @Future(message = "封禁截止时间必须晚于当前时间")
    private LocalDateTime banTime;
}
