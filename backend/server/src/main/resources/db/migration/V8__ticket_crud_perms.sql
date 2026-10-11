-- =====================================================
-- V8: 工单 CRUD 补全 —— 编辑/删除按钮权限
-- =====================================================
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (63, 57, '编辑工单', NULL, NULL, 'B', 'ticket:edit', NULL, 4, 1, 1, 0),
       (64, 57, '删除工单', NULL, NULL, 'B', 'ticket:delete', NULL, 5, 1, 1, 0);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 63), (1, 64);
