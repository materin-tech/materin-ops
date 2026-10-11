-- =====================================================
-- V1: RBAC 基础表结构（MySQL 8 / InnoDB / utf8mb4）
-- =====================================================

-- 系统用户表
CREATE TABLE sys_user
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(64)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 哈希）',
    nickname    VARCHAR(64)  NULL COMMENT '昵称',
    email       VARCHAR(128) NULL COMMENT '邮箱',
    phone       VARCHAR(32)  NULL COMMENT '手机号',
    avatar      VARCHAR(255) NULL COMMENT '头像地址',
    status      TINYINT      DEFAULT 1 COMMENT '状态：1启用 / 0禁用',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删除 / 1已删除',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统用户';

-- 系统角色表
CREATE TABLE sys_role
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(64)  NOT NULL COMMENT '角色名称',
    code       VARCHAR(64)  NOT NULL COMMENT '角色编码',
    sort       INT          DEFAULT 0 COMMENT '排序',
    status     TINYINT      DEFAULT 1 COMMENT '状态：1启用 / 0禁用',
    remark     VARCHAR(255) NULL COMMENT '备注',
    deleted    TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删除 / 1已删除',
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统角色';

-- 系统菜单表（目录 / 菜单 / 按钮）
CREATE TABLE sys_menu
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id  BIGINT       DEFAULT 0 COMMENT '父级菜单 ID，顶层为 0',
    name       VARCHAR(64)  NOT NULL COMMENT '菜单名称',
    path       VARCHAR(128) NULL COMMENT '路由路径',
    component  VARCHAR(255) NULL COMMENT '前端组件路径',
    type       CHAR(1)      NOT NULL COMMENT '类型：D目录 / M菜单 / B按钮',
    perm       VARCHAR(128) NULL COMMENT '权限标识，如 system:user:add',
    icon       VARCHAR(64)  NULL COMMENT '图标',
    sort       INT          DEFAULT 0 COMMENT '排序',
    visible    TINYINT      DEFAULT 1 COMMENT '是否可见：1是 / 0否',
    status     TINYINT      DEFAULT 1 COMMENT '状态：1启用 / 0禁用',
    deleted    TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删除 / 1已删除',
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_parent (parent_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统菜单';

-- 用户-角色关联表
CREATE TABLE sys_user_role
(
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    role_id BIGINT NOT NULL COMMENT '角色 ID',
    PRIMARY KEY (user_id, role_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户角色关联';

-- 角色-菜单关联表
CREATE TABLE sys_role_menu
(
    role_id BIGINT NOT NULL COMMENT '角色 ID',
    menu_id BIGINT NOT NULL COMMENT '菜单 ID',
    PRIMARY KEY (role_id, menu_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色菜单关联';
