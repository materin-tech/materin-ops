package com.materin.tech.ops.module.system.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 系统用户。
 * <p>deleted 字段配合 mybatis-flex 逻辑删除配置自动生效；createdAt/updatedAt 由数据库维护，不在实体中映射。</p>
 */
@Data
@Table("sys_user")
public class SysUser {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    /** 状态：1启用 / 0禁用 */
    private Integer status;

    /** 逻辑删除：0未删除 / 1已删除 */
    private Integer deleted;
}
