package com.zlf.fufuzhibo.server.service;

import com.zlf.fufuzhibo.pojo.dto.UserLoginDTO;
import com.zlf.fufuzhibo.pojo.entity.User;

public interface UserService {

    /**
     * 用户登录
     */
    User login(UserLoginDTO userLoginDTO);
}
