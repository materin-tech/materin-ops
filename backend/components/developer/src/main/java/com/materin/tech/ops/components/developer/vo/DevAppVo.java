package com.materin.tech.ops.components.developer.vo;

import lombok.Data;

/** 开发者应用视图。 */
@Data
public class DevAppVo {

    private Long id;

    private String name;

    private String appKey;

    private String appSecret;

    private String description;

    /** 状态：1启用 / 0禁用 */
    private Integer status;
}
