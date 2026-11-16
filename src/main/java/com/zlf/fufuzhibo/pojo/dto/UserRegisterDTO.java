package com.zlf.fufuzhibo.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "UserRegisterDTO", description = "用户注册DTO")
public class UserRegisterDTO {

    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 20, message = "账号长度需在4-20位之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在6-20位之间")
    private String password;
}
