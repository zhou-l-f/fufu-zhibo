package com.zlf.fufuzhibo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理端直播间列表项
 */
@Data
@Schema(name = "AdminLiveRoomVO", description = "管理端直播间列表项")
public class AdminLiveRoomVO implements Serializable {

    @Schema(description = "直播间 id")
    private Long roomId;

    @Schema(description = "主播用户 id")
    private Long userId;

    @Schema(description = "主播昵称")
    private String anchorName;

    @Schema(description = "推流流名")
    private String streamName;

    @Schema(description = "直播间状态：0 未开播、1 直播中、2 封禁")
    private Integer status;

    @Schema(description = "封禁截止时间，未封禁时为空")
    private LocalDateTime banTime;

    @Schema(description = "最近一次开播时间")
    private LocalDateTime lastStartTime;

    @Schema(description = "最近一次下播时间")
    private LocalDateTime lastEndTime;

    @Schema(description = "直播间创建时间")
    private LocalDateTime createTime;
}
