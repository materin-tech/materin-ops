package com.materin.tech.ops.components.ops.requirement.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 产品模块（产品内树形结构）。
 */
@Data
@Table("product_module")
public class ProductModule {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long productId;

    /** 父模块 ID，顶层为 0 */
    private Long parentId;

    private String name;

    private Integer sort;

    /** 逻辑删除：0未删除 / 1已删除 */
    private Integer deleted;
}
