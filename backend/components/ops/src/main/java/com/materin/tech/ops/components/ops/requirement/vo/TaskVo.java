package com.materin.tech.ops.components.ops.requirement.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 任务视图对象。 */
@Data
public class TaskVo {

    private Long id;

    private Long projectId;

    private String projectName;

    private Long iterationId;

    private String iterationName;

    private Long storyId;

    private String storyTitle;

    private String title;

    private String description;

    /** 类型：0开发 / 1测试 / 2设计 / 3其他 */
    private Integer type;

    /** 状态：0待处理 / 1进行中 / 2已完成 / 3已关闭 */
    private Integer status;

    /** 优先级：0=P1 ... 3=P4 */
    private Integer priority;

    private Long assigneeId;

    /** 被指派人昵称 */
    private String assigneeName;

    private BigDecimal planHours;

    private BigDecimal actualHours;

    private LocalDate planStart;

    private LocalDate planEnd;

}
