package com.zlf.fufuzhibo.server.controller.admin;

import com.zlf.fufuzhibo.common.constant.JwtClaimsConstants;
import com.zlf.fufuzhibo.common.properties.JwtProperties;
import com.zlf.fufuzhibo.common.result.Result;
import com.zlf.fufuzhibo.common.utils.JwtUtil;
import com.zlf.fufuzhibo.pojo.dto.AdminLoginDTO;
import com.zlf.fufuzhibo.pojo.entity.Admin;
import com.zlf.fufuzhibo.pojo.vo.AdminLoginVO;
import com.zlf.fufuzhibo.server.service.AdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


@Tag(name = "管理员管理接口")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/admin")
public class AdminController {

    private final AdminService adminService;
    private final JwtProperties jwtProperties;

    /**
     * 管理员登录
     * @param adminLoginDTO
     * @return
     */
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@RequestBody AdminLoginDTO adminLoginDTO){
        log.info("管理员登录: {}", adminLoginDTO);
        Admin admin = adminService.login(adminLoginDTO);

        //生成JWT令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstants.ADMIN_ID, admin.getId());

        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminExpireTime(),
                claims);

        AdminLoginVO adminLoginVO =AdminLoginVO.builder().
                token(token).adminId(admin.getId()).name(admin.getName()).build();

        return Result.success(adminLoginVO);
    }
}
