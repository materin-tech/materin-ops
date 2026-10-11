package com.materin.tech.ops.common.notice;

/**
 * 通知发送 SPI：任何组件需要发通知时依赖本接口，由 notice 组件提供实现。
 */
public interface NoticeSender {

    /**
     * 发送一条通知给指定用户。
     */
    void send(NoticeMessage message);
}
