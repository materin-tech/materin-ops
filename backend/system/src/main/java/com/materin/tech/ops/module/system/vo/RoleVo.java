package com.materin.tech.ops.module.system.vo;

import lombok.Data;

import java.util.List;

/**
 * 角色视图（含菜单绑定信息）。
 */
@Data
public class RoleVo {

    private Long id;

    private String name;

    private String code;

    private Integer sort;

    private Integer status;

    private String remark;

    /** 绑定的菜单 ID 集合 */
    private List<Long> menuIds;
}
