package com.materin.tech.ops.module.system.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 用户-角色关联（复合主键 user_id + role_id，无独立自增 id，不参与逻辑删除）。
 */
@Data
@Table("sys_user_role")
public class SysUserRole {

    private Long userId;

    private Long roleId;
}
