package com.materin.tech.ops.components.developer.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 开放接口清单（knife4j /v3/api-docs 同步 + 手工登记）。
 */
@Data
@Table("dev_api")
public class DevApi {

    @Id(keyType = KeyType.Auto)
    private Long id;

    /** HTTP 方法（大写） */
    private String method;

    private String path;

    /** 接口名称（OpenAPI summary） */
    private String name;

    /** 接口分组（OpenAPI tag） */
    private String tag;

    /** 开放状态：1开放 / 0关闭 */
    private Integer status;

    /** 来源：sync 同步 / manual 手工 */
    private String source;

    /** 逻辑删除：0未删除 / 1已删除 */
    private Integer deleted;
}
