package com.materin.tech.ops.module.system.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 角色-菜单关联（复合主键 role_id + menu_id，无独立自增 id，不参与逻辑删除）。
 */
@Data
@Table("sys_role_menu")
public class SysRoleMenu {

    private Long roleId;

    private Long menuId;
}
