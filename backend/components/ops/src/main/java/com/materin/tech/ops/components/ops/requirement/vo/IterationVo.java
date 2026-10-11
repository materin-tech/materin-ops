package com.materin.tech.ops.components.ops.requirement.vo;

import lombok.Data;

import java.time.LocalDate;

/** 迭代视图对象。 */
@Data
public class IterationVo {

    private Long id;

    private Long projectId;

    private String projectName;

    private String name;

    /** 状态：0未开始 / 1进行中 / 2已结束 */
    private Integer status;

    private LocalDate planStart;

    private LocalDate planEnd;

    private LocalDate actualStart;

    private LocalDate actualEnd;
}
