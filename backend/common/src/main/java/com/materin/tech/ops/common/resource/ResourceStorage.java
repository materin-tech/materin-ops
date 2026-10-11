package com.materin.tech.ops.common.resource;

/**
 * 文件存储 SPI：由具体存储组件实现（local / obs / minio），
 * resource-core 按配置路由到对应实现。
 */
public interface ResourceStorage {

    /** 存储类型标识：local / obs / minio */
    String type();

    /**
     * 存储文件，返回存储键（下载/删除时使用）。
     */
    String upload(ResourceData data);

    /**
     * 按存储键读取文件内容。
     */
    byte[] download(String key);

    /**
     * 按存储键删除文件。
     */
    void delete(String key);
}
