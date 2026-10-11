package com.materin.tech.ops.components.notice.channel;

import com.materin.tech.ops.components.notice.entity.NotifyChannel;

/**
 * IM 渠道发送 SPI：每个渠道（企微/钉钉/飞书）一个实现。
 */
public interface ImChannelSender {

    /** 渠道类型标识 */
    String type();

    /**
     * 推送 markdown 消息到群机器人。
     *
     * @return 机器人接口的响应文本（用于判断成败）
     * @throws Exception 网络或接口错误
     */
    String send(NotifyChannel channel, String title, String markdownContent) throws Exception;
}
