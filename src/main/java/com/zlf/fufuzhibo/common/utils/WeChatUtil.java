package com.zlf.fufuzhibo.common.utils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.zlf.fufuzhibo.common.properties.WeChatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeChatUtil {

    private static final String JSCODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    private final WeChatProperties weChatProperties;
    private final JsonMapper jsonMapper;

    public String getOpenid(String code) {
        String url = String.format(JSCODE2SESSION_URL,
                weChatProperties.getAppid(),
                weChatProperties.getSecret(),
                URLEncoder.encode(code, StandardCharsets.UTF_8));
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("微信登录接口请求失败，code:{}，status:{}", code, response.statusCode());
                throw new RuntimeException("微信登录接口调用失败");
            }
            JsonNode json = jsonMapper.readTree(response.body());
            if (json.has("errcode") && json.get("errcode").asInt() != 0) {
                log.error("微信登录接口返回错误，errcode:{}，errmsg:{}",
                        json.get("errcode").asInt(), json.path("errmsg").stringValue());
                throw new RuntimeException("微信登录失败：" + json.path("errmsg").stringValue());
            }
            return json.get("openid").stringValue();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用微信登录接口异常", e);
            throw new RuntimeException("微信登录失败", e);
        }
    }
}