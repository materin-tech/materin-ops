package com.materin.tech.ops.components.ops.requirement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增/编辑产品模块请求体。 */
@Data
public class ProductModuleBody {

    /** 父模块 ID，顶层传 0 或不传 */
    private Long parentId;

    @NotBlank(message = "模块名称不能为空")
    @Size(max = 128, message = "模块名称过长")
    private String name;

    private Integer sort;
}
