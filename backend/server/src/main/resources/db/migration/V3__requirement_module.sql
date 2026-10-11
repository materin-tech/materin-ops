-- =====================================================
-- V3: 需求管理模块（ITR 工单，类禅道）
-- 8 张表：product / product_module / story / project /
--         project_product / iteration / task / product_member
-- 状态口径：
--   story    0草稿 / 1评审中 / 2已确认 / 3已关闭
--   priority 0=P1最高 ... 3=P4最低（数字越小越优先）
--   project  0归档 / 1进行中 / 2已完结
--   iteration 0未开始 / 1进行中 / 2已结束
--   task.type  0开发 / 1测试 / 2设计 / 3其他
--   task.status 0待处理 / 1进行中 / 2已完成 / 3已关闭
-- =====================================================

-- 产品表
CREATE TABLE product
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128) NOT NULL COMMENT '产品名称',
    code        VARCHAR(64)  NOT NULL COMMENT '产品编码',
    description VARCHAR(512) NULL COMMENT '产品描述',
    status      TINYINT      DEFAULT 1 COMMENT '状态：1启用 / 0停用',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删除 / 1已删除',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='产品';

-- 产品模块表（树形，产品内功能模块划分）
CREATE TABLE product_module
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT       NOT NULL COMMENT '所属产品 ID',
    parent_id  BIGINT       DEFAULT 0 COMMENT '父模块 ID，顶层为 0',
    name       VARCHAR(128) NOT NULL COMMENT '模块名称',
    sort       INT          DEFAULT 0 COMMENT '排序',
    deleted    TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删除 / 1已删除',
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_product (product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='产品模块';

-- 需求表（story）
CREATE TABLE story
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT       NOT NULL COMMENT '所属产品 ID',
    module_id   BIGINT       NULL COMMENT '所属产品模块 ID',
    title       VARCHAR(256) NOT NULL COMMENT '需求标题',
    description TEXT         NULL COMMENT '需求描述',
    status      TINYINT      DEFAULT 0 COMMENT '状态：0草稿 / 1评审中 / 2已确认 / 3已关闭',
    priority    TINYINT      DEFAULT 2 COMMENT '优先级：0=P1 ... 3=P4',
    owner_id    BIGINT       NULL COMMENT '需求负责人用户 ID',
    sort        INT          DEFAULT 0 COMMENT '排序',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删除 / 1已删除',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_product (product_id),
    KEY idx_module (module_id),
    KEY idx_owner (owner_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='需求';

-- 项目表
CREATE TABLE project
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128) NOT NULL COMMENT '项目名称',
    code        VARCHAR(64)  NOT NULL COMMENT '项目编码',
    description VARCHAR(512) NULL COMMENT '项目描述',
    status      TINYINT      DEFAULT 1 COMMENT '状态：0归档 / 1进行中 / 2已完结',
    start_date  DATE         NULL COMMENT '计划开始日期',
    end_date    DATE         NULL COMMENT '计划结束日期',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删除 / 1已删除',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='项目';

-- 项目-产品关联表（物理关联表，无逻辑删除）
CREATE TABLE project_product
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '项目 ID',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_project_product (project_id, product_id),
    KEY idx_product (product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='项目产品关联';

-- 迭代表
CREATE TABLE iteration
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id   BIGINT       NOT NULL COMMENT '所属项目 ID',
    name         VARCHAR(128) NOT NULL COMMENT '迭代名称',
    status       TINYINT      DEFAULT 0 COMMENT '状态：0未开始 / 1进行中 / 2已结束',
    plan_start   DATE         NULL COMMENT '计划开始日期',
    plan_end     DATE         NULL COMMENT '计划结束日期',
    actual_start DATE         NULL COMMENT '实际开始日期',
    actual_end   DATE         NULL COMMENT '实际结束日期',
    deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删除 / 1已删除',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_project (project_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='迭代';

-- 任务表（project_id 冗余：数据权限链路 task→project→project_product→product）
CREATE TABLE task
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id   BIGINT       NOT NULL COMMENT '所属项目 ID（冗余，数据权限用）',
    iteration_id BIGINT       NULL COMMENT '所属迭代 ID',
    story_id     BIGINT       NULL COMMENT '关联需求 ID，可空',
    title        VARCHAR(256) NOT NULL COMMENT '任务标题',
    description  TEXT         NULL COMMENT '任务描述',
    type         TINYINT      DEFAULT 0 COMMENT '类型：0开发 / 1测试 / 2设计 / 3其他',
    status       TINYINT      DEFAULT 0 COMMENT '状态：0待处理 / 1进行中 / 2已完成 / 3已关闭',
    priority     TINYINT      DEFAULT 2 COMMENT '优先级：0=P1 ... 3=P4',
    assignee_id  BIGINT       NULL COMMENT '指派给用户 ID',
    plan_hours   DECIMAL(8, 1) NULL COMMENT '预计工时（小时）',
    actual_hours DECIMAL(8, 1) NULL COMMENT '实际工时（小时）',
    plan_start   DATE         NULL COMMENT '计划开始日期',
    plan_end     DATE         NULL COMMENT '计划结束日期',
    deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删除 / 1已删除',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_project (project_id),
    KEY idx_iteration (iteration_id),
    KEY idx_story (story_id),
    KEY idx_assignee (assignee_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务';

-- 产品成员授权表（数据权限依据；物理表，物理删除，避免逻辑删除撞唯一键）
CREATE TABLE product_member
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT       NOT NULL COMMENT '产品 ID',
    user_id    BIGINT       NOT NULL COMMENT '用户 ID',
    role       VARCHAR(32)  DEFAULT 'member' COMMENT '产品角色：owner负责人 / qa测试 / dev开发 / member成员',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_product (user_id, product_id),
    KEY idx_product (product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='产品成员授权';

-- =====================================================
-- 菜单种子：目录(14) + 菜单(15-19) + 按钮(20-40)
-- =====================================================

-- 顶层目录
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (14, 0, '需求管理', '/requirement', NULL, 'D', NULL, 'layers', 1, 1, 1, 0);

-- 子菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (15, 14, '产品管理', '/requirement/products', 'requirement/product/index', 'M', 'requirement:product:list', 'package', 1, 1, 1, 0),
       (16, 14, '需求列表', '/requirement/stories', 'requirement/story/index', 'M', 'requirement:story:list', 'lightbulb', 2, 1, 1, 0),
       (17, 14, '项目管理', '/requirement/projects', 'requirement/project/index', 'M', 'requirement:project:list', 'folder-kanban', 3, 1, 1, 0),
       (18, 14, '迭代管理', '/requirement/iterations', 'requirement/iteration/index', 'M', 'requirement:iteration:list', 'refresh-cw', 4, 1, 1, 0),
       (19, 14, '任务管理', '/requirement/tasks', 'requirement/task/index', 'M', 'requirement:task:list', 'clipboard-list', 5, 1, 1, 0);

-- 按钮：产品管理
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (20, 15, '新增产品', NULL, NULL, 'B', 'requirement:product:add', NULL, 1, 1, 1, 0),
       (21, 15, '编辑产品', NULL, NULL, 'B', 'requirement:product:edit', NULL, 2, 1, 1, 0),
       (22, 15, '删除产品', NULL, NULL, 'B', 'requirement:product:delete', NULL, 3, 1, 1, 0),
       (23, 15, '产品成员授权', NULL, NULL, 'B', 'requirement:product:member', NULL, 4, 1, 1, 0);

-- 按钮：需求管理
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (24, 16, '新增需求', NULL, NULL, 'B', 'requirement:story:add', NULL, 1, 1, 1, 0),
       (25, 16, '编辑需求', NULL, NULL, 'B', 'requirement:story:edit', NULL, 2, 1, 1, 0),
       (26, 16, '删除需求', NULL, NULL, 'B', 'requirement:story:delete', NULL, 3, 1, 1, 0),
       (27, 16, '需求状态流转', NULL, NULL, 'B', 'requirement:story:status', NULL, 4, 1, 1, 0);

-- 按钮：项目管理
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (28, 17, '新增项目', NULL, NULL, 'B', 'requirement:project:add', NULL, 1, 1, 1, 0),
       (29, 17, '编辑项目', NULL, NULL, 'B', 'requirement:project:edit', NULL, 2, 1, 1, 0),
       (30, 17, '删除项目', NULL, NULL, 'B', 'requirement:project:delete', NULL, 3, 1, 1, 0),
       (31, 17, '项目关联产品', NULL, NULL, 'B', 'requirement:project:product', NULL, 4, 1, 1, 0);

-- 按钮：迭代管理
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (32, 18, '新增迭代', NULL, NULL, 'B', 'requirement:iteration:add', NULL, 1, 1, 1, 0),
       (33, 18, '编辑迭代', NULL, NULL, 'B', 'requirement:iteration:edit', NULL, 2, 1, 1, 0),
       (34, 18, '删除迭代', NULL, NULL, 'B', 'requirement:iteration:delete', NULL, 3, 1, 1, 0),
       (35, 18, '迭代状态流转', NULL, NULL, 'B', 'requirement:iteration:status', NULL, 4, 1, 1, 0);

-- 按钮：任务管理
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (36, 19, '新增任务', NULL, NULL, 'B', 'requirement:task:add', NULL, 1, 1, 1, 0),
       (37, 19, '编辑任务', NULL, NULL, 'B', 'requirement:task:edit', NULL, 2, 1, 1, 0),
       (38, 19, '删除任务', NULL, NULL, 'B', 'requirement:task:delete', NULL, 3, 1, 1, 0),
       (39, 19, '任务状态流转', NULL, NULL, 'B', 'requirement:task:status', NULL, 4, 1, 1, 0),
       (40, 19, '任务指派', NULL, NULL, 'B', 'requirement:task:assign', NULL, 5, 1, 1, 0);

-- ---------- 角色绑定菜单 ----------
-- super_admin 绑定需求管理全部菜单（14-40）
INSERT INTO sys_role_menu (role_id, menu_id)
VALUES (1, 14),
       (1, 15), (1, 16), (1, 17), (1, 18), (1, 19),
       (1, 20), (1, 21), (1, 22), (1, 23),
       (1, 24), (1, 25), (1, 26), (1, 27),
       (1, 28), (1, 29), (1, 30), (1, 31),
       (1, 32), (1, 33), (1, 34), (1, 35),
       (1, 36), (1, 37), (1, 38), (1, 39), (1, 40);
