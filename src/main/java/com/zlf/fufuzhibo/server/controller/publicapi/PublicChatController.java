package com.zlf.fufuzhibo.server.controller.publicapi;

import com.zlf.fufuzhibo.common.result.Result;
import com.zlf.fufuzhibo.pojo.vo.ChatMessageVO;
import com.zlf.fufuzhibo.server.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 直播间聊天历史接口，匿名可读；发言走 WebSocket
 */
@Tag(name = "公开聊天接口", description = "无需登录即可查看的直播间聊天历史")
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/public/live-rooms")
public class PublicChatController {

    private final ChatService chatService;

    @Operation(summary = "查询聊天历史",
            description = "聊天按直播场次隔离，sessionId 必传（可从直播间详情的 sessionId 获取）。"
                    + "按场次内 seq 游标向前翻页，返回结果按时间正序。首次不传 lastSeq 返回最新一页；"
                    + "继续上翻时传当前最早一条消息的 seq 作为 lastSeq。")
    @GetMapping("/{roomId}/messages")
    public Result<List<ChatMessageVO>> listMessages(
            @PathVariable @Positive(message = "直播间 id 不合法") Long roomId,
            @RequestParam @Positive(message = "直播场次 id 不合法") Long sessionId,
            @RequestParam(required = false)
            @Positive(message = "消息游标不合法") Long lastSeq,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 50, message = "每页条数不能超过 50") Integer size) {
        log.info("查询聊天历史: roomId={}, sessionId={}, lastSeq={}, size={}",
                roomId, sessionId, lastSeq, size);
        return Result.success(chatService.listHistory(roomId, sessionId, lastSeq, size));
    }
}
