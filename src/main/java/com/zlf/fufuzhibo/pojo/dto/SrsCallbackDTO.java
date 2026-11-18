package com.zlf.fufuzhibo.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * SRS HTTP 回调报文
 */
@Data
public class SrsCallbackDTO {

    private String action;

    @JsonProperty("client_id")
    private String clientId;

    private String ip;

    private String vhost;

    private String app;

    private String stream;

    /**
     * 推流地址中的查询串，例如 ?token=xxx
     */
    private String param;

    @JsonProperty("tcUrl")
    private String tcUrl;
}
