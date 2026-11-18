package com.zlf.fufuzhibo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(name = "StartLiveVO", description = "申请开始直播返回结果")
public class StartLiveVO {

    private Long roomId;

    private Long sessionId;

    private String streamName;

    /**
     * 推流服务地址，OBS 中填写到“服务器”
     */
    private String serverUrl;

    /**
     * 推流密钥，OBS 中填写到“串流密钥”，内含短效 token
     */
    private String streamKey;

    private String rtmpPlayUrl;

    private String httpFlvPlayUrl;

    private String hlsPlayUrl;
}
