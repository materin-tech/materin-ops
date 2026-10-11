package com.materin.tech.ops.module.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态路由视图（供后端动态路由模式使用，与 vben5 路由结构对齐）。
 */
@Data
public class RouteVo {

    /** 路由名称 */
    private String name;

    /** 路由路径 */
    private String path;

    /** 前端组件路径 */
    private String component;

    private Meta meta;

    private List<RouteVo> children = new ArrayList<>();

    /**
     * 路由元信息。
     */
    @Data
    public static class Meta {

        /** 菜单标题 */
        private String title;

        private String icon;

        /** 排序 */
        private Integer order;
    }
}
