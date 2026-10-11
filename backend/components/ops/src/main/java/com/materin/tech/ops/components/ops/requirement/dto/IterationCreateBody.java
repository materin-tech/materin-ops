package com.materin.tech.ops.components.ops.requirement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 新增迭代请求体（初始状态为未开始）。 */
@Data
public class IterationCreateBody {

    @NotNull(message = "所属项目不能为空")
    private Long projectId;

    @NotBlank(message = "迭代名称不能为空")
    @Size(max = 128, message = "迭代名称过长")
    private String name;

    private LocalDate planStart;

    private LocalDate planEnd;
}
