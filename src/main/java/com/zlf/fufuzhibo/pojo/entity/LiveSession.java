package com.zlf.fufuzhibo.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推流会话：每次推流一条记录，用于历史记录、时长统计与回调幂等
 */
@Data
@TableName("live_session")
public class LiveSession {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("room_id")
    private Long roomId;

    @TableField("user_id")
    private Long userId;

    /**
     * 本场直播标题
     */
    private String title;

    /**
     * 本场直播封面 URL
     */
    @TableField("cover_url")
    private String coverUrl;

    /**
     * SRS 推流客户端 id，on_publish 时写入
     */
    @TableField("client_id")
    private String clientId;

    @TableField("stream_name")
    private String streamName;

    /**
     * 0 待开播、1 直播中、2 已结束
     */
    private Integer status;

    /**
     * 推流开始时间，on_publish 时写入
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 推流时长（秒）
     */
    private Integer duration;

    /**
     * 结束原因：1 正常断流、2 超时回收、3 异常
     */
    @TableField("end_reason")
    private Integer endReason;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
