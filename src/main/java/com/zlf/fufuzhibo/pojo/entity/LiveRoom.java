package com.zlf.fufuzhibo.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 直播间：一个用户一个固定直播间，长期存在，多次开播由 live_session 记录
 */
@Data
@TableName("live_room")
public class LiveRoom {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("stream_name")
    private String streamName;

    /**
     * 0 未开播、1 直播中、2 封禁
     */
    private Integer status;

    @TableField("current_client_id")
    private String currentClientId;

    @TableField("last_start_time")
    private LocalDateTime lastStartTime;

    @TableField("last_end_time")
    private LocalDateTime lastEndTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
