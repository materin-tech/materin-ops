package com.materin.tech.ops.components.developer.vo;

import lombok.Data;

/** 开放接口视图。 */
@Data
public class DevApiVo {

    private Long id;

    private String method;

    private String path;

    private String name;

    private String tag;

    /** 开放状态：1开放 / 0关闭 */
    private Integer status;

    /** 来源：sync / manual */
    private String source;
}
