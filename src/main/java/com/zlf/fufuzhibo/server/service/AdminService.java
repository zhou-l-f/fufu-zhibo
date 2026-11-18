package com.zlf.fufuzhibo.server.service;

import com.zlf.fufuzhibo.pojo.dto.AdminLoginDTO;
import com.zlf.fufuzhibo.pojo.vo.AdminLoginVO;
import com.zlf.fufuzhibo.pojo.vo.AdminProfileVO;

public interface AdminService {

    /**
     * 管理员登录，校验通过后签发 JWT 令牌
     *
     * @param adminLoginDTO 登录信息
     * @return 登录结果，包含令牌与基础资料
     */
    AdminLoginVO login(AdminLoginDTO adminLoginDTO);

    /**
     * 获取指定管理员的基础信息
     *
     * @param adminId 管理员 id
     * @return 管理员姓名、状态与最近登录时间
     */
    AdminProfileVO getProfile(Long adminId);
}
