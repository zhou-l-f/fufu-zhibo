package com.zlf.fufuzhibo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(name = "LiveRoomVO", description = "直播间信息")
public class LiveRoomVO {

    private Long id;

    private Long userId;

    private String streamName;

    private Integer status;
}
