package com.zlf.fufuzhibo.server.service;

import com.zlf.fufuzhibo.pojo.dto.AdminLoginDTO;
import com.zlf.fufuzhibo.pojo.vo.AdminLoginVO;

public interface AdminService {

    /**
     * 管理员登录，校验通过后签发 JWT 令牌
     *
     * @param adminLoginDTO 登录信息
     * @return 登录结果，包含令牌与基础资料
     */
    AdminLoginVO login(AdminLoginDTO adminLoginDTO);
}
