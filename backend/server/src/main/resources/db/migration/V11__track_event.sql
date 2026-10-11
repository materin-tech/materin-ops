-- =====================================================
-- V11: 前端埋点事件（点击/页面浏览/自定义行为）
-- =====================================================
CREATE TABLE track_event
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    trace_id   VARCHAR(64)  NULL COMMENT '链路 ID（前后端一致）',
    session_id VARCHAR(64)  NOT NULL COMMENT '前端会话 ID',
    user_id    BIGINT       NULL COMMENT '用户 ID（未登录为空）',
    client     VARCHAR(16)  NOT NULL COMMENT '端：web / h5',
    event      VARCHAR(64)  NOT NULL COMMENT '事件类型：pv / click / custom',
    page       VARCHAR(256) NULL COMMENT '页面路由',
    target     VARCHAR(256) NULL COMMENT '点击目标（元素标识/文本）',
    props      VARCHAR(1024) NULL COMMENT '扩展属性 JSON',
    ua         VARCHAR(256) NULL COMMENT 'User-Agent 摘要',
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
    KEY idx_session (session_id),
    KEY idx_event (event),
    KEY idx_created (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='前端埋点事件';
