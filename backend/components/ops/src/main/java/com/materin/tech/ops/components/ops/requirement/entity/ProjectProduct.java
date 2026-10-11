package com.materin.tech.ops.components.ops.requirement.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 项目-产品关联（物理关联表，无逻辑删除）。
 */
@Data
@Table("project_product")
public class ProjectProduct {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long projectId;

    private Long productId;
}
