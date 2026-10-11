package com.materin.tech.ops.module.system.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 系统菜单（目录 D / 菜单 M / 按钮 B）。
 */
@Data
@Table("sys_menu")
public class SysMenu {

    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 父级菜单 ID，顶层为 0 */
    private Long parentId;

    private String name;

    private String path;

    private String component;

    /** 类型：D目录 / M菜单 / B按钮 */
    private String type;

    /** 权限标识，如 system:user:add */
    private String perm;

    private String icon;

    private Integer sort;

    /** 是否可见：1是 / 0否 */
    private Integer visible;

    /** 状态：1启用 / 0禁用 */
    private Integer status;

    /** 逻辑删除：0未删除 / 1已删除 */
    private Integer deleted;
}
