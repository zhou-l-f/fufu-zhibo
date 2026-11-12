package com.zlf.fufuzhibo.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin")
public class Admin {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;

    @JsonIgnore
    private String password;

    private String name;

    @TableField("id_card_enc")
    private byte[] idCardEnc;

    @TableField("id_card_hash")
    private String idCardHash;

    private Integer status;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}