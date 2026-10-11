-- =====================================================
-- V4: 开发者中心（开放 API）
-- 3 张表：dev_app（第三方应用）/ dev_api（开放接口清单）
--         dev_app_api（应用-接口授权，物理表）
-- APP 鉴权：X-App-Key + HMAC-SHA256 签名（timestamp + nonce 防重放），
--           与登录体系（Sa-Token）并行，同一路径双通道。
-- =====================================================

-- 开发者应用表
CREATE TABLE dev_app
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128) NOT NULL COMMENT '应用名称',
    app_key     VARCHAR(64)  NOT NULL COMMENT '应用 AppKey（对外标识）',
    app_secret  VARCHAR(128) NOT NULL COMMENT '应用 AppSecret（HMAC 签名密钥）',
    description VARCHAR(512) NULL COMMENT '应用描述',
    status      TINYINT      DEFAULT 1 COMMENT '状态：1启用 / 0禁用',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删除 / 1已删除',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_app_key (app_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='开发者应用';

-- 开放接口清单表（通过 knife4j /v3/api-docs 同步 + 手工登记）
CREATE TABLE dev_api
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    method     VARCHAR(8)   NOT NULL COMMENT 'HTTP 方法（大写，如 GET/POST）',
    path       VARCHAR(256) NOT NULL COMMENT '接口路径，如 /requirement/products',
    name       VARCHAR(256) NULL COMMENT '接口名称（来自 OpenAPI summary）',
    tag        VARCHAR(128) NULL COMMENT '接口分组（来自 OpenAPI tag）',
    status     TINYINT      DEFAULT 0 COMMENT '开放状态：1开放 / 0关闭',
    source     VARCHAR(16)  DEFAULT 'sync' COMMENT '来源：sync 同步 / manual 手工',
    deleted    TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删除 / 1已删除',
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_method_path (method, path)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='开放接口清单';

-- 应用-接口授权表（物理表，物理删除）
CREATE TABLE dev_app_api
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_id     BIGINT NOT NULL COMMENT '应用 ID',
    api_id     BIGINT NOT NULL COMMENT '接口 ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_app_api (app_id, api_id),
    KEY idx_api (api_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='应用接口授权';

-- =====================================================
-- 菜单种子：目录(41) + 菜单(42-44) + 按钮(45-49)
-- =====================================================

INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (41, 0, '开发者中心', '/developer', NULL, 'D', NULL, 'terminal', 2, 1, 1, 0);

INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (42, 41, '应用管理', '/developer/apps', 'developer/app/index', 'M', 'developer:app:list', 'app-window', 1, 1, 1, 0),
       (43, 41, '接口管理', '/developer/apis', 'developer/api/index', 'M', 'developer:api:list', 'braces', 2, 1, 1, 0),
       (44, 41, '授权管理', '/developer/authorizations', 'developer/auth/index', 'M', 'developer:auth:list', 'key-round', 3, 1, 1, 0);

-- 按钮：应用管理
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (45, 42, '新增应用', NULL, NULL, 'B', 'developer:app:add', NULL, 1, 1, 1, 0),
       (46, 42, '编辑应用', NULL, NULL, 'B', 'developer:app:edit', NULL, 2, 1, 1, 0),
       (47, 42, '删除应用', NULL, NULL, 'B', 'developer:app:delete', NULL, 3, 1, 1, 0);

-- 按钮：接口管理 / 授权管理
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (48, 43, '同步接口', NULL, NULL, 'B', 'developer:api:sync', NULL, 1, 1, 1, 0),
       (49, 43, '维护接口', NULL, NULL, 'B', 'developer:api:edit', NULL, 2, 1, 1, 0),
       (50, 44, '接口授权', NULL, NULL, 'B', 'developer:auth:grant', NULL, 1, 1, 1, 0);

-- super_admin 绑定开发者中心全部菜单（41-50）
INSERT INTO sys_role_menu (role_id, menu_id)
VALUES (1, 41), (1, 42), (1, 43), (1, 44),
       (1, 45), (1, 46), (1, 47), (1, 48), (1, 49), (1, 50);
