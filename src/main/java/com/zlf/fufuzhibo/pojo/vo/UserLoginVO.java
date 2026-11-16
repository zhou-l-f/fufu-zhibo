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
@Schema(name = "UserLoginVO", description = "用户登录VO")
public class UserLoginVO {
    private String token;

    private String name;

    private String avatarUrl;

    // 性别：0 未知、1 男、2 女
    private Integer gender;

}