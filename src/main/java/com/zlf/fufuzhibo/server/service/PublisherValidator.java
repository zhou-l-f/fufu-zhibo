package com.zlf.fufuzhibo.server.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlf.fufuzhibo.common.constant.MessageConstant;
import com.zlf.fufuzhibo.common.constant.StatusConstant;
import com.zlf.fufuzhibo.common.exception.BaseException;
import com.zlf.fufuzhibo.pojo.entity.User;
import com.zlf.fufuzhibo.pojo.entity.UserIdentity;
import com.zlf.fufuzhibo.server.mapper.UserIdentityMapper;
import com.zlf.fufuzhibo.server.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 主播开播资格校验：账号正常 + 已完成实名认证
 */
@Component
@RequiredArgsConstructor
public class PublisherValidator {

    private final UserMapper userMapper;
    private final UserIdentityMapper userIdentityMapper;

    public void check(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !StatusConstant.ENABLE.equals(user.getStatus())) {
            throw new BaseException(MessageConstant.PUSH_USER_DISABLED);
        }

        Long identityCount = userIdentityMapper.selectCount(
                Wrappers.<UserIdentity>lambdaQuery()
                        .eq(UserIdentity::getUserId, userId)
                        .eq(UserIdentity::getStatus, StatusConstant.ENABLE));
        if (identityCount == null || identityCount == 0) {
            throw new BaseException(MessageConstant.PUSH_IDENTITY_REQUIRED);
        }
    }
}
