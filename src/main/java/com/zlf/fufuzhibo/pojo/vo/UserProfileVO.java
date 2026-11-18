package com.zlf.fufuzhibo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 当前登录用户资料
 */
@Data
@Builder
@Schema(name = "UserProfileVO", description = "当前登录用户资料")
public class UserProfileVO implements Serializable {

    @Schema(description = "用户 id")
    private Long id;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "昵称")
    private String name;

    @Schema(description = "性别：0 未知、1 男、2 女")
    private Integer gender;

    @Schema(description = "头像 URL")
    private String avatarUrl;

    @Schema(description = "账号状态：0 禁用、1 正常")
    private Integer status;

    @Schema(description = "是否已完成实名认证")
    private Boolean verified;
}
