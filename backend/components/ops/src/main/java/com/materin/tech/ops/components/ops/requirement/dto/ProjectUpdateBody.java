package com.materin.tech.ops.components.ops.requirement.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 编辑项目请求体（编码创建后不可改）。 */
@Data
public class ProjectUpdateBody {

    @Size(max = 128, message = "项目名称过长")
    private String name;

    @Size(max = 512, message = "项目描述过长")
    private String description;

    private Integer status;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 关联产品 ID 集合（全量覆盖） */
    private List<Long> productIds;
}
