package com.materin.tech.ops.components.ops.requirement.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 产品成员授权（数据权限依据）。物理表、物理删除，避免逻辑删除撞唯一键。
 */
@Data
@Table("product_member")
public class ProductMember {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long productId;

    private Long userId;

    /** 产品角色：owner负责人 / qa测试 / dev开发 / member成员 */
    private String role;
}
