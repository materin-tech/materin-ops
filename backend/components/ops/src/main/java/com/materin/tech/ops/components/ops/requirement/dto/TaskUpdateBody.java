package com.materin.tech.ops.components.ops.requirement.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 编辑任务请求体（不含项目与状态，状态流转走独立接口）。 */
@Data
public class TaskUpdateBody {

    private Long iterationId;

    private Long storyId;

    @Size(max = 256, message = "任务标题过长")
    private String title;

    private String description;

    private Integer type;

    private Integer priority;

    private BigDecimal planHours;

    private BigDecimal actualHours;

    private LocalDate planStart;

    private LocalDate planEnd;
}
