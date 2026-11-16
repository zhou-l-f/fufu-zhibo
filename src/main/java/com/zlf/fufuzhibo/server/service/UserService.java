package com.zlf.fufuzhibo.server.service;

import com.zlf.fufuzhibo.pojo.dto.UserIdentityDTO;
import com.zlf.fufuzhibo.pojo.dto.UserLoginDTO;
import com.zlf.fufuzhibo.pojo.dto.UserRegisterDTO;
import com.zlf.fufuzhibo.pojo.entity.User;

public interface UserService {

    /**
     * 用户登录
     */
    User login(UserLoginDTO userLoginDTO);

    /**
     * 用户注册
     */
    void register(UserRegisterDTO userRegisterDTO);
    /**
     * 用户实名认证
     *
     * @param userIdentityDTO 实名认证信息
     * @param userId          当前登录用户id
     */
    void verify(UserIdentityDTO userIdentityDTO, Long userId);
}
