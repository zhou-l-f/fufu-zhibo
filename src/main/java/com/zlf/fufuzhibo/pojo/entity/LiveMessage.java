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
     * 所属直播场次，消息按场次隔离，不同场次的聊天不会串
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * 场次内消息序号，由 Redis INCR 生成，作为历史分页游标
     */
    private Long seq;

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
