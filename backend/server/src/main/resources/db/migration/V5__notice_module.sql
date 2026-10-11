-- =====================================================
-- V5: 通知模块（站内通知中心）
-- notice 表 + 通知发送按钮权限
-- =====================================================

CREATE TABLE notice
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(256) NOT NULL COMMENT '通知标题',
    content     TEXT         NULL COMMENT '通知内容',
    type        VARCHAR(32)  DEFAULT 'system' COMMENT '类型：system系统 / announcement公告 / todo待办',
    level       VARCHAR(16)  DEFAULT 'info' COMMENT '级别：info / warn / error',
    receiver_id BIGINT       NOT NULL COMMENT '接收人用户 ID',
    sender_id   BIGINT       NULL COMMENT '发送者用户 ID，系统发送为空',
    link        VARCHAR(256) NULL COMMENT '点击跳转的前端路由',
    read_flag   TINYINT      DEFAULT 0 COMMENT '已读：0未读 / 1已读',
    read_at     DATETIME     NULL COMMENT '阅读时间',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删除 / 1已删除',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_receiver (receiver_id),
    KEY idx_read_flag (read_flag)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='站内通知';

-- 按钮：发送通知（挂在系统管理目录下）
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (51, 1, '发送通知', NULL, NULL, 'B', 'notice:send', NULL, 9, 1, 1, 0);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 51);
