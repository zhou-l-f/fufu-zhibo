package com.zlf.fufuzhibo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 直播间详情，供匿名进入直播间使用
 */
@Data
@Schema(name = "LiveRoomDetailVO", description = "直播间详情")
public class LiveRoomDetailVO implements Serializable {

    @Schema(description = "直播间 id")
    private Long roomId;

    @Schema(description = "当前直播场次 id")
    private Long sessionId;

    @Schema(description = "本场直播标题")
    private String title;

    @Schema(description = "本场直播封面 URL")
    private String coverUrl;

    @Schema(description = "推流流名")
    private String streamName;

    @Schema(description = "主播昵称")
    private String anchorName;

    @Schema(description = "在线观看人数，来自 Redis 计数，缺失时为 0")
    private Integer viewerCount;

    @Schema(description = "本场直播开始时间")
    private LocalDateTime startTime;

    @Schema(description = "HTTP-FLV 播放地址")
    private String httpFlvPlayUrl;

    @Schema(description = "HLS 播放地址，SRS 未开启 HLS 时为空")
    private String hlsPlayUrl;
}
