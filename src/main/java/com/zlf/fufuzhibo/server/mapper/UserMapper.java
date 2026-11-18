package com.zlf.fufuzhibo.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlf.fufuzhibo.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("select * from user where username = #{username}")
    User getByUsername(String username);

    @Update("UPDATE `user` SET last_login_time = #{lastLoginTime} WHERE id = #{id}")
    void updateLastLoginTime(@Param("id") Long id,
                            @Param("lastLoginTime") LocalDateTime lastLoginTime);

    /**
     * 按需更新个人信息，只更新传入的非空字段
     */
    @Update("""
            <script>
            UPDATE `user`
            <set>
                <if test="name != null">`name` = #{name},</if>
                <if test="gender != null">gender = #{gender},</if>
                <if test="avatarUrl != null">avatar_url = #{avatarUrl},</if>
                update_time = NOW()
            </set>
            WHERE id = #{id}
            </script>
            """)
    int updateProfile(@Param("id") Long id,
                      @Param("name") String name,
                      @Param("gender") Integer gender,
                      @Param("avatarUrl") String avatarUrl);
}