package com.zlf.fufuzhibo.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 直播间聊天消息，全量落库，最近消息另存于 Redis
 */
@Data
@TableName("live_message")
public class LiveMessage {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("room_id")
    private Long roomId;

    /**
     * 所属直播场次，发送时取当前直播中的场次，可为空
     */
    @TableField("session_id")
    private Long sessionId;

    @TableField("user_id")
    private Long userId;

    private String content;

    /**
     * 1 正常、0 已删除
     */
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
