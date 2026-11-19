package com.zlf.fufuzhibo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 直播间聊天消息，供 WebSocket 推送与历史接口共用
 */
@Data
@Schema(name = "ChatMessageVO", description = "直播间聊天消息")
public class ChatMessageVO implements Serializable {

    @Schema(description = "消息 id，批量入库前可能为空，前端不要用于排序")
    private Long id;

    @Schema(description = "场次内消息序号，自增且连续，作为历史分页游标")
    private Long seq;

    @Schema(description = "直播间 id")
    private Long roomId;

    @Schema(description = "所属直播场次 id")
    private Long sessionId;

    @Schema(description = "发言用户 id")
    private Long userId;

    @Schema(description = "发言用户昵称")
    private String userName;

    @Schema(description = "发言用户头像 URL")
    private String avatarUrl;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "发送时间")
    private LocalDateTime createTime;
}
