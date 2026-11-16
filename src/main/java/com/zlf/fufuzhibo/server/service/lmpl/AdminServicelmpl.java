package com.zlf.fufuzhibo.server.service.lmpl;

import com.zlf.fufuzhibo.common.constant.MessageConstant;
import com.zlf.fufuzhibo.common.constant.StatusConstant;
import com.zlf.fufuzhibo.common.exception.AccountLockedException;
import com.zlf.fufuzhibo.common.exception.AccountNotFoundException;
import com.zlf.fufuzhibo.common.exception.PasswordErrorException;
import com.zlf.fufuzhibo.pojo.dto.AdminLoginDTO;
import com.zlf.fufuzhibo.pojo.entity.Admin;
import com.zlf.fufuzhibo.server.mapper.AdminMapper;
import com.zlf.fufuzhibo.server.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServicelmpl implements AdminService {
    /**
     * 管理员登录
     *
     * @param adminLoginDTO
     */

    private final AdminMapper adminMapper;

    public static final BCryptPasswordEncoder passwordEncoder =new BCryptPasswordEncoder();

    @Override
    public Admin login(AdminLoginDTO adminLoginDTO) {

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

        return admin;
    }
}
