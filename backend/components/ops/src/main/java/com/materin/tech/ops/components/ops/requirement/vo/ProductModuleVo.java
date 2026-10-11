package com.materin.tech.ops.components.ops.requirement.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 产品模块树节点。 */
@Data
public class ProductModuleVo {

    private Long id;

    private Long productId;

    private Long parentId;

    private String name;

    private Integer sort;

    private List<ProductModuleVo> children = new ArrayList<>();
}
