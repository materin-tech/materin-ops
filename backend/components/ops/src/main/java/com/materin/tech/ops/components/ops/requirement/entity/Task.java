package com.materin.tech.ops.components.ops.requirement.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 任务。project_id 冗余存储，用于数据权限链路 task → project → project_product → product。
 */
@Data
@Table("task")
public class Task {

    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 所属项目 ID（冗余，数据权限用） */
    private Long projectId;

    private Long iterationId;

    private Long storyId;

    private String title;

    private String description;

    /** 类型：0开发 / 1测试 / 2设计 / 3其他 */
    private Integer type;

    /** 状态：0待处理 / 1进行中 / 2已完成 / 3已关闭 */
    private Integer status;

    /** 优先级：0=P1 ... 3=P4 */
    private Integer priority;

    private Long assigneeId;

    private BigDecimal planHours;

    private BigDecimal actualHours;

    private LocalDate planStart;

    private LocalDate planEnd;

    /** 逻辑删除：0未删除 / 1已删除 */
    private Integer deleted;
}
