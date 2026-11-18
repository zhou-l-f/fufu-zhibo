package com.zlf.fufuzhibo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 主播端“我的直播间”信息
 */
@Data
@Builder
@Schema(name = "MyLiveRoomVO", description = "我的直播间信息")
public class MyLiveRoomVO implements Serializable {

    @Schema(description = "是否已创建直播间")
    private Boolean hasRoom;

    @Schema(description = "直播间 id")
    private Long roomId;

    @Schema(description = "主播用户 id")
    private Long userId;

    @Schema(description = "推流流名")
    private String streamName;

    @Schema(description = "直播间状态：0 未开播、1 直播中、2 封禁")
    private Integer roomStatus;

    @Schema(description = "当前是否正在直播")
    private Boolean live;

    @Schema(description = "当前场次 id，无进行中场次时为空")
    private Long sessionId;

    @Schema(description = "当前场次状态：0 待开播、1 直播中")
    private Integer sessionStatus;

    @Schema(description = "当前场次标题")
    private String title;

    @Schema(description = "当前场次封面 URL")
    private String coverUrl;

    @Schema(description = "本场开始时间")
    private LocalDateTime startTime;

    @Schema(description = "在线观看人数，缺失时为 0")
    private Integer viewerCount;
}
