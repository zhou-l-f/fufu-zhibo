package com.zlf.fufuzhibo.server.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlf.fufuzhibo.common.constant.JwtClaimsConstants;
import com.zlf.fufuzhibo.common.constant.MessageConstant;
import com.zlf.fufuzhibo.common.constant.StatusConstant;
import com.zlf.fufuzhibo.common.exception.AccountLockedException;
import com.zlf.fufuzhibo.common.exception.AccountNotFoundException;
import com.zlf.fufuzhibo.common.exception.BaseException;
import com.zlf.fufuzhibo.common.exception.PasswordErrorException;
import com.zlf.fufuzhibo.common.properties.JwtProperties;
import com.zlf.fufuzhibo.common.utils.AliOssUtil;
import com.zlf.fufuzhibo.common.utils.CryptoUtil;
import com.zlf.fufuzhibo.common.utils.JwtUtil;
import com.zlf.fufuzhibo.pojo.dto.UserIdentityDTO;
import com.zlf.fufuzhibo.pojo.dto.UserLoginDTO;
import com.zlf.fufuzhibo.pojo.dto.UserProfileDTO;
import com.zlf.fufuzhibo.pojo.dto.UserRegisterDTO;
import com.zlf.fufuzhibo.pojo.entity.User;
import com.zlf.fufuzhibo.pojo.entity.UserIdentity;
import com.zlf.fufuzhibo.pojo.vo.UserLoginVO;
import com.zlf.fufuzhibo.pojo.vo.UserProfileVO;
import com.zlf.fufuzhibo.server.mapper.UserIdentityMapper;
import com.zlf.fufuzhibo.server.mapper.UserMapper;
import com.zlf.fufuzhibo.server.service.LoginAttemptLimiter;
import com.zlf.fufuzhibo.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    public static final BCryptPasswordEncoder passwordEncoder =new BCryptPasswordEncoder();

    private final UserIdentityMapper userIdentityMapper;
    private final CryptoUtil cryptoUtil;
    private final AliOssUtil aliOssUtil;
    private final JwtProperties jwtProperties;
    private final LoginAttemptLimiter loginAttemptLimiter;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[\\dX]$");
    private static final int[] ID_CARD_WEIGHTS =
            {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] ID_CARD_CHECK_CODES =
            {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};


    /**
     * 用户登录：校验账号密码与状态，通过后签发 JWT 令牌
     * @param userLoginDTO 登录信息
     */
    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO) {
        String username = userLoginDTO.getUsername();
        // 连续失败达到阈值后锁定，避免被撞库
        loginAttemptLimiter.check("user", username);

        User user = userMapper.getByUsername(username);

        if(user == null){
            loginAttemptLimiter.recordFailure("user", username);
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        
        // 校验密码
        String password = userLoginDTO.getPassword();
        boolean matched = passwordEncoder.matches(password, user.getPassword());
        if (!matched) {
            loginAttemptLimiter.recordFailure("user", username);
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if(user.getStatus().equals(StatusConstant.DISABLE))
        {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }
        loginAttemptLimiter.clear("user", username);
        userMapper.updateLastLoginTime(user.getId(), LocalDateTime.now(ZONE));

        //生成JWT令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstants.USER_ID, user.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserExpireTime(),
                claims);

        return UserLoginVO.builder()
                .name(user.getName())
                .token(token)
                .gender(user.getGender())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    /**
     * 查询当前登录用户资料，附带实名认证状态
     */
    @Override
    public UserProfileVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        Long identityCount = userIdentityMapper.selectCount(
                Wrappers.<UserIdentity>lambdaQuery().eq(UserIdentity::getUserId, userId));

        return UserProfileVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .gender(user.getGender())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .verified(identityCount != null && identityCount > 0)
                .build();
    }

    /**
     * 用户注册
     * @param userRegisterDTO
     */
    @Override
    public void register(UserRegisterDTO userRegisterDTO) {
        String username = userRegisterDTO.getUsername().trim();

        if (userMapper.getByUsername(username) != null) {
            throw new BaseException(MessageConstant.USERNAME_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(userRegisterDTO.getPassword()));
        user.setName(username);
        user.setGender(0);
        user.setStatus(StatusConstant.ENABLE);

        userMapper.insert(user);
    }

    /**
     * 完善个人信息：只有传入的字段才会更新
     *
     * @param userId 当前登录用户 id
     * @param dto    个人信息
     * @return 最新头像 URL
     */
    @Override
    public String updateProfile(Long userId, UserProfileDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        String name = dto.getName() == null ? null : dto.getName().trim();
        if (name != null && name.isEmpty()) {
            name = null;
        }

        String avatarUrl = null;
        if (dto.getAvatar() != null && !dto.getAvatar().isEmpty()) {
            avatarUrl = aliOssUtil.uploadImage(dto.getAvatar(), "avatar");
        }

        if (name == null && dto.getGender() == null && avatarUrl == null) {
            return user.getAvatarUrl();
        }

        userMapper.updateProfile(userId, name, dto.getGender(), avatarUrl);
        return avatarUrl != null ? avatarUrl : user.getAvatarUrl();
    }

    /**
     * 用户实名认证
     * @param userIdentityDTO 实名认证信息
     * @param userId          当前登录用户id
     */
    @Override
    public void verify(UserIdentityDTO userIdentityDTO, Long userId) {
        String phone = userIdentityDTO.getPhone().trim();
        String idCard = userIdentityDTO.getIdCard().trim().toUpperCase(Locale.ROOT);

        checkPhone(phone);
        checkIdCard(idCard);

        Long submitted = userIdentityMapper.selectCount(
                Wrappers.<UserIdentity>lambdaQuery().eq(UserIdentity::getUserId, userId));
        if (submitted > 0) {
            throw new BaseException(MessageConstant.IDENTITY_ALREADY_EXISTS);
        }

        String phoneHash = cryptoUtil.hmac(phone);
        String idCardHash = cryptoUtil.hmac(idCard);

        Long phoneBound = userIdentityMapper.selectCount(
                Wrappers.<UserIdentity>lambdaQuery().eq(UserIdentity::getPhoneHash, phoneHash));
        if (phoneBound > 0) {
            throw new BaseException(MessageConstant.PHONE_ALREADY_BOUND);
        }

        Long idCardBound = userIdentityMapper.selectCount(
                Wrappers.<UserIdentity>lambdaQuery().eq(UserIdentity::getIdCardHash, idCardHash));
        if (idCardBound > 0) {
            throw new BaseException(MessageConstant.ID_CARD_ALREADY_BOUND);
        }

        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setUserId(userId);
        userIdentity.setRealName(userIdentityDTO.getRealName().trim());
        userIdentity.setPhoneEnc(cryptoUtil.encrypt(phone));
        userIdentity.setPhoneHash(phoneHash);
        userIdentity.setIdCardEnc(cryptoUtil.encrypt(idCard));
        userIdentity.setIdCardHash(idCardHash);
        // 提交即认证通过
        userIdentity.setStatus(StatusConstant.ENABLE);

        userIdentityMapper.insert(userIdentity);
    }

    private void checkPhone(String phone) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BaseException(MessageConstant.INVALID_PHONE);
        }
    }

    private void checkIdCard(String idCard) {
        if (!ID_CARD_PATTERN.matcher(idCard).matches() || !checkIdCardCode(idCard)) {
            throw new BaseException(MessageConstant.INVALID_ID_CARD);
        }
    }

    /**
     * 按 GB 11643 规则校验身份证最后一位校验码
     */
    private boolean checkIdCardCode(String idCard) {
        int sum = 0;
        for (int i = 0; i < ID_CARD_WEIGHTS.length; i++) {
            sum += (idCard.charAt(i) - '0') * ID_CARD_WEIGHTS[i];
        }
        return ID_CARD_CHECK_CODES[sum % 11] == idCard.charAt(17);
    }
}
