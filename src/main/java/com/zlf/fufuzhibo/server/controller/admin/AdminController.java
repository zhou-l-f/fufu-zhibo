package com.zlf.fufuzhibo.server.controller.admin;

import com.zlf.fufuzhibo.common.result.Result;
import com.zlf.fufuzhibo.common.utils.SecurityUtil;
import com.zlf.fufuzhibo.pojo.dto.AdminLoginDTO;
import com.zlf.fufuzhibo.pojo.vo.AdminLoginVO;
import com.zlf.fufuzhibo.pojo.vo.AdminProfileVO;
import com.zlf.fufuzhibo.server.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "管理员管理接口")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    /**
     * 管理员登录
     * @param adminLoginDTO 管理员登录信息
     * @return 管理员登录VO
     */
    @Operation(summary = "管理员登录",
            description = "使用管理员账号和密码登录，成功后返回 JWT 令牌以及管理员 id、姓名等基础信息。")
    @PostMapping("/sessions")
    public Result<AdminLoginVO> login(@RequestBody AdminLoginDTO adminLoginDTO){
        log.info("管理员登录: {}", adminLoginDTO);
        return Result.success(adminService.login(adminLoginDTO));
    }

    /**
     * 获取当前登录管理员信息
     */
    @Operation(summary = "获取当前管理员信息",
            description = "根据请求头中的管理员令牌返回姓名、账号状态与最近登录时间。")
    @GetMapping("/me")
    public Result<AdminProfileVO> getCurrentAdmin() {
        Long adminId = SecurityUtil.currentUserId();
        log.info("获取当前管理员信息: adminId={}", adminId);
        return Result.success(adminService.getProfile(adminId));
    }
}
