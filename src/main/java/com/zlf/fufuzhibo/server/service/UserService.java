package com.zlf.fufuzhibo.server.service;

import com.zlf.fufuzhibo.pojo.dto.UserIdentityDTO;
import com.zlf.fufuzhibo.pojo.dto.UserLoginDTO;
import com.zlf.fufuzhibo.pojo.dto.UserProfileDTO;
import com.zlf.fufuzhibo.pojo.dto.UserRegisterDTO;
import com.zlf.fufuzhibo.pojo.vo.UserLoginVO;

public interface UserService {

    /**
     * 用户登录，校验通过后签发 JWT 令牌
     *
     * @param userLoginDTO 登录信息
     * @return 登录结果，包含令牌与基础资料
     */
    UserLoginVO login(UserLoginDTO userLoginDTO);

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

    /**
     * 完善个人信息：按需修改昵称、性别、头像
     *
     * @param userId 当前登录用户 id
     * @param dto    个人信息
     * @return 最新头像 URL
     */
    String updateProfile(Long userId, UserProfileDTO dto);
}
