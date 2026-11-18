package com.zlf.fufuzhibo.server.controller.user;

import com.zlf.fufuzhibo.common.constant.JwtClaimsConstants;
import com.zlf.fufuzhibo.common.constant.MessageConstant;
import com.zlf.fufuzhibo.common.exception.BaseException;
import com.zlf.fufuzhibo.common.properties.JwtProperties;
import com.zlf.fufuzhibo.common.result.Result;
import com.zlf.fufuzhibo.common.utils.JwtUtil;
import com.zlf.fufuzhibo.pojo.dto.UserIdentityDTO;
import com.zlf.fufuzhibo.pojo.dto.UserLoginDTO;
import com.zlf.fufuzhibo.pojo.dto.UserRegisterDTO;
import com.zlf.fufuzhibo.pojo.entity.User;
import com.zlf.fufuzhibo.pojo.vo.UserLoginVO;
import com.zlf.fufuzhibo.server.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    /**
     * 用户注册
     * @param userRegisterDTO 用户注册信息DTO
     * @return 无返回值
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid UserRegisterDTO userRegisterDTO){
        log.info("用户注册: username={}", userRegisterDTO.getUsername());
        userService.register(userRegisterDTO);
        return Result.success();
    }

    /**
     * 用户实名认证
     * @param userIdentityDTO 用户实名认证信息DTO
     * @return 无返回值
     */
    @PostMapping("/identity")
    public Result<Void> identity(@RequestBody @Valid UserIdentityDTO userIdentityDTO){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal == null) {
            throw new BaseException(MessageConstant.USER_NOT_LOGIN);
        }
        Long userId = Long.valueOf(principal.toString());
        log.info("用户实名认证: userId={}", userId);
        userService.verify(userIdentityDTO, userId);
        return Result.success();
    }

    /**
     * 用户登录
     * @param userLoginDTO 用户登录信息DTO
     * @return 用户登录VO
     */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody @Valid UserLoginDTO userLoginDTO){
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
