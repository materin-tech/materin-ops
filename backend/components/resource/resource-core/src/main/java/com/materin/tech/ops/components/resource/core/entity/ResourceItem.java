package com.materin.tech.ops.components.resource.core.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 文件资源元数据。
 */
@Data
@Table("resource")
public class ResourceItem {

    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 原始文件名 */
    private String originalName;

    private String contentType;

    /** 文件大小（字节） */
    private Long size;

    /** 存储类型：local / obs / minio */
    private String storageType;

    /** 存储键（下载/删除定位） */
    private String storageKey;

    /** 上传人用户 ID */
    private Long uploaderId;

    /** 逻辑删除：0未删除 / 1已删除 */
    private Integer deleted;
}
