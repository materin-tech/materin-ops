package com.materin.tech.ops.components.notice.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 通知渠道（企业微信 / 钉钉 / 飞书群机器人）。
 */
@Data
@Table("notify_channel")
public class NotifyChannel {

    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 渠道类型：wecom / dingtalk / feishu */
    private String type;

    private String name;

    /** 机器人 Webhook 地址 */
    private String webhookUrl;

    /** 加签密钥（钉钉安全设置，可空） */
    private String secret;

    /** 启用：1是 / 0否 */
    private Integer enabled;

    /** 逻辑删除：0未删除 / 1已删除 */
    private Integer deleted;
}
