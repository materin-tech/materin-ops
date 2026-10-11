package com.materin.tech.ops.components.ops.requirement.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 编辑迭代请求体（不含项目与状态）。 */
@Data
public class IterationUpdateBody {

    @Size(max = 128, message = "迭代名称过长")
    private String name;

    private LocalDate planStart;

    private LocalDate planEnd;
}
