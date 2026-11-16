package com.zlf.fufuzhibo.server.service;

import com.zlf.fufuzhibo.pojo.dto.AdminLoginDTO;
import com.zlf.fufuzhibo.pojo.entity.Admin;

public interface AdminService {

    /**
     * 管理员登录
     * @param adminLoginDTO
     */
    Admin login(AdminLoginDTO adminLoginDTO);
}
