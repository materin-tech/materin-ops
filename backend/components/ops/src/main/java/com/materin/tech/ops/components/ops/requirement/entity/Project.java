package com.materin.tech.ops.components.ops.requirement.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDate;

/**
 * 项目。
 */
@Data
@Table("project")
public class Project {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String name;

    private String code;

    private String description;

    /** 状态：0归档 / 1进行中 / 2已完结 */
    private Integer status;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 逻辑删除：0未删除 / 1已删除 */
    private Integer deleted;
}
