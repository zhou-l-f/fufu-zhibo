package com.zlf.fufuzhibo.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "UserLoginDTO", description = "用户登录DTO")
public class UserLoginDTO {

    @NotBlank(message = "登录凭证不能为空")
    private String code;
}