-- =====================================================
-- V7: 工单模块（ITR + SLA）
-- ticket / ticket_action / sla_policy + 菜单
-- 状态：0待受理 / 1处理中 / 2待确认 / 3已解决 / 4已关闭
-- =====================================================

CREATE TABLE sla_policy
(
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                    VARCHAR(128) NOT NULL COMMENT '策略名称',
    priority                TINYINT      NOT NULL COMMENT '适用优先级：0=P1 ... 3=P4',
    first_response_minutes  INT          NOT NULL COMMENT '首次响应时限（分钟）',
    resolve_minutes         INT          NOT NULL COMMENT '解决时限（分钟）',
    enabled                 TINYINT      DEFAULT 1 COMMENT '启用：1是 / 0否',
    deleted                 TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删除 / 1已删除',
    created_at              DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at              DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_priority (priority)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='SLA 策略';

CREATE TABLE ticket
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_no           VARCHAR(32)  NOT NULL COMMENT '工单号，如 TK-20261010-0001',
    title               VARCHAR(256) NOT NULL COMMENT '工单标题',
    description         TEXT         NULL COMMENT '问题描述',
    type                TINYINT      DEFAULT 0 COMMENT '类型：0故障 / 1咨询 / 2变更 / 3其他',
    priority            TINYINT      DEFAULT 2 COMMENT '优先级：0=P1 ... 3=P4',
    status              TINYINT      DEFAULT 0 COMMENT '状态：0待受理 / 1处理中 / 2待确认 / 3已解决 / 4已关闭',
    reporter_id         BIGINT       NOT NULL COMMENT '报障人用户 ID',
    assignee_id         BIGINT       NULL COMMENT '处理人用户 ID',
    sla_policy_id       BIGINT       NULL COMMENT '命中的 SLA 策略 ID',
    response_deadline   DATETIME     NULL COMMENT '首次响应截止时间',
    resolve_deadline    DATETIME     NULL COMMENT '解决截止时间',
    first_response_at   DATETIME     NULL COMMENT '实际首次响应时间',
    resolved_at         DATETIME     NULL COMMENT '实际解决时间',
    closed_at           DATETIME     NULL COMMENT '关闭时间',
    deleted             TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删除 / 1已删除',
    created_at          DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_status (status),
    KEY idx_reporter (reporter_id),
    KEY idx_assignee (assignee_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='工单';

CREATE TABLE ticket_action
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id  BIGINT       NOT NULL COMMENT '工单 ID',
    user_id    BIGINT       NULL COMMENT '操作人用户 ID',
    action     TINYINT      NOT NULL COMMENT '动作：0创建 / 1受理 / 2转派 / 3备注 / 4状态变更 / 5解决 / 6关闭',
    content    VARCHAR(1024) NULL COMMENT '备注内容 / 说明',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_ticket (ticket_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='工单操作流水';

-- 默认 SLA 策略
INSERT INTO sla_policy (id, name, priority, first_response_minutes, resolve_minutes, enabled, deleted)
VALUES (1, 'P1 紧急', 0, 30, 240, 1, 0),
       (2, 'P2 高', 1, 60, 480, 1, 0),
       (3, 'P3 中', 2, 120, 1440, 1, 0),
       (4, 'P4 低', 3, 480, 2880, 1, 0);

-- 菜单：工单管理
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (56, 0, '工单管理', '/ticket', NULL, 'D', NULL, 'life-buoy', 3, 1, 1, 0);

INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (57, 56, '工单列表', '/ticket/tickets', 'ticket/index', 'M', 'ticket:list', 'inbox', 1, 1, 1, 0),
       (58, 56, 'SLA 策略', '/ticket/slas', 'ticket/sla/index', 'M', 'ticket:sla:list', 'timer', 2, 1, 1, 0);

INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (59, 57, '创建工单', NULL, NULL, 'B', 'ticket:create', NULL, 1, 1, 1, 0),
       (60, 57, '处理工单', NULL, NULL, 'B', 'ticket:handle', NULL, 2, 1, 1, 0),
       (61, 57, '关闭工单', NULL, NULL, 'B', 'ticket:close', NULL, 3, 1, 1, 0),
       (62, 58, '编辑 SLA', NULL, NULL, 'B', 'ticket:sla:edit', NULL, 1, 1, 1, 0);

INSERT INTO sys_role_menu (role_id, menu_id)
VALUES (1, 56), (1, 57), (1, 58), (1, 59), (1, 60), (1, 61), (1, 62);
