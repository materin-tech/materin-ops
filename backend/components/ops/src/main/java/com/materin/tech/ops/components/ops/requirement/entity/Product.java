package com.materin.tech.ops.components.ops.requirement.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 产品。
 * <p>deleted 字段配合 mybatis-flex 逻辑删除配置自动生效；createdAt/updatedAt 由数据库维护，不在实体中映射。</p>
 */
@Data
@Table("product")
public class Product {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String name;

    private String code;

    private String description;

    /** 状态：1启用 / 0停用 */
    private Integer status;

    /** 逻辑删除：0未删除 / 1已删除 */
    private Integer deleted;
}
