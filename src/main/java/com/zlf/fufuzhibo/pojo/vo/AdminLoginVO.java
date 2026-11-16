package com.zlf.fufuzhibo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AdminLoginVO", description = "管理员登录VO")
public class AdminLoginVO {

    private String token;

    private Long adminId;

    private String username;

    private String name;
}