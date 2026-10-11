-- =====================================================
-- V6: 文件资源模块 + 需求描述富文本扩容 + 任务看板/资源管理菜单
-- =====================================================

-- 文件资源元数据表
CREATE TABLE resource
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_name VARCHAR(512) NOT NULL COMMENT '原始文件名',
    content_type  VARCHAR(128) NULL COMMENT 'MIME 类型',
    size          BIGINT       DEFAULT 0 COMMENT '文件大小（字节）',
    storage_type  VARCHAR(16)  NOT NULL COMMENT '存储类型：local / obs / minio',
    storage_key   VARCHAR(512) NOT NULL COMMENT '存储键',
    uploader_id   BIGINT       NULL COMMENT '上传人用户 ID',
    deleted       TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删除 / 1已删除',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_uploader (uploader_id),
    KEY idx_storage (storage_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='文件资源';

-- 需求/任务描述扩容为 MEDIUMTEXT（富文本 HTML）
ALTER TABLE story MODIFY description MEDIUMTEXT NULL COMMENT '需求描述（富文本）';
ALTER TABLE task MODIFY description MEDIUMTEXT NULL COMMENT '任务描述（富文本）';

-- 菜单：系统管理下"资源管理"
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (52, 1, '资源管理', '/system/resources', 'system/resource/index', 'M', 'resource:list', 'hard-drive', 6, 1, 1, 0);

-- 按钮：资源上传 / 删除
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (53, 52, '上传资源', NULL, NULL, 'B', 'resource:upload', NULL, 1, 1, 1, 0),
       (54, 52, '删除资源', NULL, NULL, 'B', 'resource:delete', NULL, 2, 1, 1, 0);

-- 菜单：需求管理下"任务看板"
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (55, 14, '任务看板', '/requirement/board', 'requirement/board/index', 'M', 'requirement:board:list', 'trello', 6, 1, 1, 0);

-- super_admin 绑定新菜单
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 52), (1, 53), (1, 54), (1, 55);
