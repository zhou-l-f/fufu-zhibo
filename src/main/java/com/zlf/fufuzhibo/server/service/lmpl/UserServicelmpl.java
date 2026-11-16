package com.zlf.fufuzhibo.server.service.lmpl;

import com.zlf.fufuzhibo.common.utils.WeChatUtil;
import com.zlf.fufuzhibo.pojo.dto.UserLoginDTO;
import com.zlf.fufuzhibo.pojo.entity.User;
import com.zlf.fufuzhibo.server.mapper.UserMapper;
import com.zlf.fufuzhibo.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class UserServicelmpl implements UserService {

    private final UserMapper userMapper;
    private final WeChatUtil weChatUtil;

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 用户登录
     * @param userLoginDTO
     */
    @Override
    public User login(UserLoginDTO userLoginDTO) {
        String code = userLoginDTO.getCode();

        // 根据code获取用户openid
        String openid = weChatUtil.getOpenid(code);

        User user = userMapper.getByOpenid(openid);

        LocalDateTime now = LocalDateTime.now(ZONE);

        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setLastLoginTime(now);
            user.setUpdateTime(now);
            userMapper.insert(user);
        } else {
            // 登录只刷新最后登录时间，不改 update_time
            user.setLastLoginTime(now);
            User update = new User();
            update.setId(user.getId());
            update.setLastLoginTime(now);
            userMapper.updateById(update);
        }
        return user;
    }
}
