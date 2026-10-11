-- =====================================================
-- V10: 通知渠道（企业微信 / 钉钉 / 飞书群机器人）
-- =====================================================
CREATE TABLE notify_channel
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    type        VARCHAR(16)  NOT NULL COMMENT '渠道类型：wecom 企业微信 / dingtalk 钉钉 / feishu 飞书',
    name        VARCHAR(128) NOT NULL COMMENT '渠道名称',
    webhook_url VARCHAR(512) NOT NULL COMMENT '机器人 Webhook 地址',
    secret      VARCHAR(256) NULL COMMENT '加签密钥（钉钉安全设置，可空）',
    enabled     TINYINT      DEFAULT 1 COMMENT '启用：1是 / 0否',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删除 / 1已删除',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_type (type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='通知渠道';

-- 菜单：系统管理下「通知渠道」+ 按钮
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (66, 1, '通知渠道', '/system/notice-channels', 'system/channel/index', 'M', 'notice:channel:list', 'send', 7, 1, 1, 0);

INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (67, 66, '维护渠道', NULL, NULL, 'B', 'notice:channel:edit', NULL, 1, 1, 1, 0),
       (68, 66, '渠道测试', NULL, NULL, 'B', 'notice:channel:test', NULL, 2, 1, 1, 0);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 66), (1, 67), (1, 68);
