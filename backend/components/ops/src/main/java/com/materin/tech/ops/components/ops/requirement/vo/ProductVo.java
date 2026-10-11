package com.materin.tech.ops.components.ops.requirement.vo;

import lombok.Data;


/** 产品视图对象。 */
@Data
public class ProductVo {

    private Long id;

    private String name;

    private String code;

    private String description;

    /** 状态：1启用 / 0停用 */
    private Integer status;

}
