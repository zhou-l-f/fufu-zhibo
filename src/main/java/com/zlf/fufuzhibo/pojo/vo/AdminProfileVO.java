package com.zlf.fufuzhibo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 当前登录管理员信息
 */
@Data
@Builder
@Schema(name = "AdminProfileVO", description = "当前登录管理员信息")
public class AdminProfileVO implements Serializable {

    @Schema(description = "管理员姓名")
    private String name;

    @Schema(description = "账号状态：1 启用、0 禁用")
    private Integer status;

    @Schema(description = "最近登录时间")
    private LocalDateTime lastLoginTime;
}
