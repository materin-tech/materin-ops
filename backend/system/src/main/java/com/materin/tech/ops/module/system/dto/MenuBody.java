package com.materin.tech.ops.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增/编辑菜单请求体。
 */
@Data
public class MenuBody {

    /** 父级菜单 ID，顶层传 0 */
    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    private String name;

    /** 路由路径（目录/菜单必填） */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 类型：D目录 / M菜单 / B按钮 */
    @NotBlank(message = "菜单类型不能为空")
    private String type;

    /** 权限标识 */
    private String perm;

    private String icon;

    private Integer sort;

    /** 是否可见：1是 / 0否 */
    private Integer visible;

    /** 状态：1启用 / 0禁用 */
    private Integer status;
}
