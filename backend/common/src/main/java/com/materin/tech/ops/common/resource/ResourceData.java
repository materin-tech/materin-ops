package com.materin.tech.ops.common.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件资源数据（跨组件通用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceData {

    /** 原始文件名 */
    private String originalName;

    /** MIME 类型 */
    private String contentType;

    /** 文件字节数组 */
    private byte[] bytes;
}
