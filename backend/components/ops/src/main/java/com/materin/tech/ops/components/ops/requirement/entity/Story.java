package com.materin.tech.ops.components.ops.requirement.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 需求（story）。
 */
@Data
@Table("story")
public class Story {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long productId;

    private Long moduleId;

    private String title;

    private String description;

    /** 状态：0草稿 / 1评审中 / 2已确认 / 3已关闭 */
    private Integer status;

    /** 优先级：0=P1 ... 3=P4 */
    private Integer priority;

    /** 需求负责人用户 ID */
    private Long ownerId;

    private Integer sort;

    /** 逻辑删除：0未删除 / 1已删除 */
    private Integer deleted;
}
