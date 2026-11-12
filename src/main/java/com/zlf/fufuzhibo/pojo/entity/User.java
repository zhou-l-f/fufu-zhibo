package com.zlf.fufuzhibo.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String openid;

    private String name;

    @TableField("avatar_url")
    private String avatarUrl;

    private Integer gender;

    @TableField("phone_enc")
    private byte[] phoneEnc;

    @TableField("phone_hash")
    private String phoneHash;

    private Integer status;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}