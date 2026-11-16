package com.zlf.fufuzhibo.server.mapper;

import com.zlf.fufuzhibo.pojo.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminMapper {

    @Select("select * from admin where username = #{username}")
    Admin getByusername(String username);
}
