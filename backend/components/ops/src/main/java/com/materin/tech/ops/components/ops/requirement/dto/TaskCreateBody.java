package com.materin.tech.ops.components.ops.requirement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 新增任务请求体（初始状态为待处理）。 */
@Data
public class TaskCreateBody {

    @NotNull(message = "所属项目不能为空")
    private Long projectId;

    /** 所属迭代 ID，可空 */
    private Long iterationId;

    /** 关联需求 ID，可空 */
    private Long storyId;

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 256, message = "任务标题过长")
    private String title;

    private String description;

    /** 类型：0开发 / 1测试 / 2设计 / 3其他 */
    private Integer type;

    /** 优先级：0=P1 ... 3=P4 */
    private Integer priority;

    /** 指派给用户 ID */
    private Long assigneeId;

    private BigDecimal planHours;

    private BigDecimal actualHours;

    private LocalDate planStart;

    private LocalDate planEnd;
}
