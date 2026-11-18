package com.zlf.fufuzhibo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 主播直播记录列表项
 */
@Data
@Builder
@Schema(name = "LiveHistoryItemVO", description = "主播直播记录列表项")
public class LiveHistoryItemVO implements Serializable {

    @Schema(description = "直播场次 id")
    private Long sessionId;

    @Schema(description = "本场直播标题")
    private String title;

    @Schema(description = "本场直播封面 URL")
    private String coverUrl;

    @Schema(description = "场次状态：0 待开播、1 直播中、2 已结束")
    private Integer status;

    @Schema(description = "开播时间，待开播场次为空")
    private LocalDateTime startTime;

    @Schema(description = "结束时间，未结束场次为空")
    private LocalDateTime endTime;

    @Schema(description = "直播时长（秒），未结束场次为空")
    private Integer duration;
}
