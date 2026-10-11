package com.materin.tech.ops.module.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树视图（管理端菜单管理使用，含按钮节点）。
 */
@Data
public class MenuTreeVo {

    private Long id;

    private Long parentId;

    private String name;

    private String path;

    private String component;

    /** 类型：D目录 / M菜单 / B按钮 */
    private String type;

    private String perm;

    private String icon;

    private Integer sort;

    /** 是否可见：1是 / 0否 */
    private Integer visible;

    private Integer status;

    private List<MenuTreeVo> children = new ArrayList<>();
}
