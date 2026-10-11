package com.materin.tech.ops.module.system.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 系统角色。
 */
@Data
@Table("sys_role")
public class SysRole {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String name;

    private String code;

    private Integer sort;

    /** 状态：1启用 / 0禁用 */
    private Integer status;

    private String remark;

    /** 逻辑删除：0未删除 / 1已删除 */
    private Integer deleted;
}
