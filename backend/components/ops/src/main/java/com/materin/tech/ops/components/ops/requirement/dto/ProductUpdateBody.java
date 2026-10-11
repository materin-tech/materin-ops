package com.materin.tech.ops.components.ops.requirement.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 编辑产品请求体（字段缺省表示不更新）。 */
@Data
public class ProductUpdateBody {

    @Size(max = 128, message = "产品名称过长")
    private String name;

    @Size(max = 64, message = "产品编码过长")
    private String code;

    @Size(max = 512, message = "产品描述过长")
    private String description;

    private Integer status;
}
