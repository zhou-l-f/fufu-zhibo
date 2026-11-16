package com.zlf.fufuzhibo.server.controller.user;

import com.zlf.fufuzhibo.common.constant.JwtClaimsConstants;
import com.zlf.fufuzhibo.common.properties.JwtProperties;
import com.zlf.fufuzhibo.common.result.Result;
import com.zlf.fufuzhibo.common.utils.JwtUtil;
import com.zlf.fufuzhibo.pojo.dto.UserLoginDTO;
import com.zlf.fufuzhibo.pojo.entity.User;
import com.zlf.fufuzhibo.pojo.vo.UserLoginVO;
import com.zlf.fufuzhibo.server.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "用户管理接口")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/user")
public class UserController {

    private final UserService userService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO){
        log.info("用户登录: {}", userLoginDTO);
        User user = userService.login(userLoginDTO);

        //生成JWT令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstants.USER_ID, user.getId());

        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserExpireTime(),
                claims);

        UserLoginVO userLoginVO = UserLoginVO.builder()
                                    .name(user.getName())
                                    .token(token)
                                    .gender(user.getGender())
                                    .avatarUrl(user.getAvatarUrl())
                                    .build();

        return Result.success(userLoginVO);
    }
}
