package com.materin.tech.ops.components.ops.requirement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增产品请求体。 */
@Data
public class ProductCreateBody {

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 128, message = "产品名称过长")
    private String name;

    @NotBlank(message = "产品编码不能为空")
    @Size(max = 64, message = "产品编码过长")
    private String code;

    @Size(max = 512, message = "产品描述过长")
    private String description;

    /** 状态：1启用 / 0停用，缺省启用 */
    private Integer status;
}
