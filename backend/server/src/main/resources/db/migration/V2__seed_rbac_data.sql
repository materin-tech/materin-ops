-- =====================================================
-- V2: RBAC 静态种子数据
-- 注意：admin 的密码为占位串，DataInitializer 启动时会
--       用 BCrypt("123456") 覆盖，避免在 SQL 中硬编码哈希。
-- =====================================================

-- ---------- 角色 ----------
INSERT INTO sys_role (id, name, code, sort, status, remark, deleted)
VALUES (1, '超级管理员', 'super_admin', 0, 1, '拥有全部权限', 0),
       (2, '普通运维', 'ops', 1, 1, '普通运维人员', 0);

-- ---------- 默认管理员用户 ----------
INSERT INTO sys_user (id, username, password, nickname, email, phone, avatar, status, deleted)
VALUES (1, 'admin', 'INIT_NEED_RESET', '系统管理员', NULL, NULL, NULL, 1, 0);

-- admin 绑定 super_admin
INSERT INTO sys_user_role (user_id, role_id)
VALUES (1, 1);

-- ---------- 菜单 ----------
-- 顶层目录
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (1, 0, '系统管理', '/system', NULL, 'D', NULL, 'settings', 0, 1, 1, 0);

-- 子菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (2, 1, '用户管理', '/system/users', 'system/user/index', 'M', 'system:user:list', 'user', 1, 1, 1, 0),
       (3, 1, '角色管理', '/system/roles', 'system/role/index', 'M', 'system:role:list', 'user-round-cog', 2, 1, 1, 0),
       (4, 1, '菜单管理', '/system/menus', 'system/menu/index', 'M', 'system:menu:list', 'menu', 3, 1, 1, 0);

-- 按钮：用户管理
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (5, 2, '新增用户', NULL, NULL, 'B', 'system:user:add', NULL, 1, 1, 1, 0),
       (6, 2, '编辑用户', NULL, NULL, 'B', 'system:user:edit', NULL, 2, 1, 1, 0),
       (7, 2, '删除用户', NULL, NULL, 'B', 'system:user:delete', NULL, 3, 1, 1, 0);

-- 按钮：角色管理
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (8, 3, '新增角色', NULL, NULL, 'B', 'system:role:add', NULL, 1, 1, 1, 0),
       (9, 3, '编辑角色', NULL, NULL, 'B', 'system:role:edit', NULL, 2, 1, 1, 0),
       (10, 3, '删除角色', NULL, NULL, 'B', 'system:role:delete', NULL, 3, 1, 1, 0);

-- 按钮：菜单管理
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (11, 4, '新增菜单', NULL, NULL, 'B', 'system:menu:add', NULL, 1, 1, 1, 0),
       (12, 4, '编辑菜单', NULL, NULL, 'B', 'system:menu:edit', NULL, 2, 1, 1, 0),
       (13, 4, '删除菜单', NULL, NULL, 'B', 'system:menu:delete', NULL, 3, 1, 1, 0);

-- ---------- 角色绑定菜单 ----------
-- super_admin 绑定全部菜单（1-13）
INSERT INTO sys_role_menu (role_id, menu_id)
VALUES (1, 1), (1, 2), (1, 3), (1, 4),
       (1, 5), (1, 6), (1, 7),
       (1, 8), (1, 9), (1, 10),
       (1, 11), (1, 12), (1, 13);

-- ops 绑定：系统管理目录 + 用户管理菜单及其按钮
INSERT INTO sys_role_menu (role_id, menu_id)
VALUES (2, 1),
       (2, 2),
       (2, 5), (2, 6), (2, 7);
