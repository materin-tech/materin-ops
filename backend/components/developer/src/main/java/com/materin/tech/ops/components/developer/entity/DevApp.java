package com.materin.tech.ops.components.developer.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 开发者应用（第三方 APP）。
 */
@Data
@Table("dev_app")
public class DevApp {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String name;

    private String appKey;

    private String appSecret;

    private String description;

    /** 状态：1启用 / 0禁用 */
    private Integer status;

    /** 逻辑删除：0未删除 / 1已删除 */
    private Integer deleted;
}
