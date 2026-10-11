package com.materin.tech.ops.components.developer.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 应用-接口授权（物理表，物理删除）。
 */
@Data
@Table("dev_app_api")
public class DevAppApi {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long appId;

    private Long apiId;
}
