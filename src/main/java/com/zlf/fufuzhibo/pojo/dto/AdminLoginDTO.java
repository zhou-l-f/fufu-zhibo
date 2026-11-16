package com.zlf.fufuzhibo.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "AdminLoginDTO", description = "管理员登录DTO")
public class AdminLoginDTO {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}