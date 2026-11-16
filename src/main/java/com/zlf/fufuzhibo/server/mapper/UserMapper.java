package com.zlf.fufuzhibo.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlf.fufuzhibo.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);
}