package com.zlf.fufuzhibo.server.service.impl;

import com.zlf.fufuzhibo.common.constant.JwtClaimsConstants;
import com.zlf.fufuzhibo.common.constant.MessageConstant;
import com.zlf.fufuzhibo.common.constant.StatusConstant;
import com.zlf.fufuzhibo.common.exception.AccountLockedException;
import com.zlf.fufuzhibo.common.exception.AccountNotFoundException;
import com.zlf.fufuzhibo.common.exception.PasswordErrorException;
import com.zlf.fufuzhibo.common.properties.JwtProperties;
import com.zlf.fufuzhibo.common.utils.JwtUtil;
import com.zlf.fufuzhibo.pojo.dto.AdminLoginDTO;
import com.zlf.fufuzhibo.pojo.entity.Admin;
import com.zlf.fufuzhibo.pojo.vo.AdminLoginVO;
import com.zlf.fufuzhibo.pojo.vo.AdminProfileVO;
import com.zlf.fufuzhibo.server.mapper.AdminMapper;
import com.zlf.fufuzhibo.server.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final JwtProperties jwtProperties;

    public static final BCryptPasswordEncoder passwordEncoder =new BCryptPasswordEncoder();

    /**
     * 管理员登录：校验账号密码与状态，通过后签发 JWT 令牌
     *
     * @param adminLoginDTO 登录信息
     * @return 登录结果，包含令牌与基础资料
     */
    @Override
    public AdminLoginVO login(AdminLoginDTO adminLoginDTO) {

        String username = adminLoginDTO.getUsername();
        Admin admin = adminMapper.getByUsername(username);
        if(admin == null){
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        String password = adminLoginDTO.getPassword();

        boolean matched = passwordEncoder.matches(password, admin.getPassword());
        if (!matched) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if(admin.getStatus().equals(StatusConstant.DISABLE))
        {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        // 记录最近登录时间，供管理员信息接口展示
        Admin loginTimeUpdate = new Admin();
        loginTimeUpdate.setId(admin.getId());
        loginTimeUpdate.setLastLoginTime(LocalDateTime.now());
        adminMapper.updateById(loginTimeUpdate);

        //生成JWT令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstants.ADMIN_ID, admin.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminExpireTime(),
                claims);

        return AdminLoginVO.builder()
                .token(token)
                .adminId(admin.getId())
                .name(admin.getName())
                .build();
    }

    @Override
    public AdminProfileVO getProfile(Long adminId) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        return AdminProfileVO.builder()
                .name(admin.getName())
                .status(admin.getStatus())
                .lastLoginTime(admin.getLastLoginTime())
                .build();
    }
}
