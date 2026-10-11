package com.materin.tech.ops.components.ops.requirement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 新增项目请求体。 */
@Data
public class ProjectCreateBody {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 128, message = "项目名称过长")
    private String name;

    @NotBlank(message = "项目编码不能为空")
    @Size(max = 64, message = "项目编码过长")
    private String code;

    @Size(max = 512, message = "项目描述过长")
    private String description;

    /** 状态：0归档 / 1进行中 / 2已完结，缺省进行中 */
    private Integer status;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 关联产品 ID 集合 */
    private List<Long> productIds;
}
