package com.zlf.fufuzhibo.server.controller.user;

import com.zlf.fufuzhibo.common.result.Result;
import com.zlf.fufuzhibo.common.utils.SecurityUtil;
import com.zlf.fufuzhibo.pojo.dto.UserIdentityDTO;
import com.zlf.fufuzhibo.pojo.dto.UserLoginDTO;
import com.zlf.fufuzhibo.pojo.dto.UserProfileDTO;
import com.zlf.fufuzhibo.pojo.dto.UserRegisterDTO;
import com.zlf.fufuzhibo.pojo.vo.UserLoginVO;
import com.zlf.fufuzhibo.pojo.vo.UserProfileVO;
import com.zlf.fufuzhibo.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户管理接口")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
     * @param userRegisterDTO 用户注册信息DTO
     * @return 无返回值
     */
    @Operation(summary = "用户注册",
            description = "使用用户名和密码注册普通用户账号，账号默认启用；用户名不可重复。")
    @PostMapping("/users")
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
    @Operation(summary = "用户实名认证",
            description = "提交真实姓名、手机号与身份证号完成实名认证，提交即认证通过；同一用户只能认证一次，手机号与身份证号不可重复绑定。")
    @PostMapping("/identities")
    public Result<Void> identity(@RequestBody @Valid UserIdentityDTO userIdentityDTO){
        Long userId = SecurityUtil.currentUserId();
        log.info("用户实名认证: userId={}", userId);
        userService.verify(userIdentityDTO, userId);
        return Result.success();
    }

    /**
     * 查询当前登录用户资料：昵称、头像、性别、账号状态与实名状态
     */
    @Operation(summary = "查询当前用户资料",
            description = "根据 token 返回当前登录用户的账号、昵称、性别、头像、账号状态以及是否已完成实名认证，用于前端刷新后恢复用户状态。")
    @GetMapping("/users/me")
    public Result<UserProfileVO> getProfile() {
        Long userId = SecurityUtil.currentUserId();
        log.info("查询用户资料: userId={}", userId);
        return Result.success(userService.getProfile(userId));
    }

    /**
     * 完善个人信息：昵称、性别、头像，只更新传入的字段
     */
    @Operation(summary = "完善个人信息",
            description = "按需修改昵称、性别和头像，只更新传入的字段；头像经阿里云 OSS 上传，返回最新头像地址。")
    @PatchMapping(value = "/users/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> updateProfile(@ModelAttribute @Valid UserProfileDTO dto) {
        Long userId = SecurityUtil.currentUserId();
        log.info("完善个人信息: userId={}", userId);
        return Result.success(userService.updateProfile(userId, dto));
    }

    /**
     * 用户登录
     * @param userLoginDTO 用户登录信息DTO
     * @return 用户登录VO
     */
    @Operation(summary = "用户登录",
            description = "使用用户名和密码登录，成功后返回 JWT 令牌以及昵称、性别、头像等基础资料。")
    @PostMapping("/sessions")
    public Result<UserLoginVO> login(@RequestBody @Valid UserLoginDTO userLoginDTO){
        log.info("用户登录: {}", userLoginDTO);
        return Result.success(userService.login(userLoginDTO));
    }
}
