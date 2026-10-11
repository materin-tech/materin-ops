package com.materin.tech.ops.components.ops.requirement.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDate;

/**
 * 迭代。
 */
@Data
@Table("iteration")
public class Iteration {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long projectId;

    private String name;

    /** 状态：0未开始 / 1进行中 / 2已结束 */
    private Integer status;

    private LocalDate planStart;

    private LocalDate planEnd;

    private LocalDate actualStart;

    private LocalDate actualEnd;

    /** 逻辑删除：0未删除 / 1已删除 */
    private Integer deleted;
}
