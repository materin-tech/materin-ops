-- =====================================================
-- V9: 工单创建独立页面入口
-- =====================================================
INSERT INTO sys_menu (id, parent_id, name, path, component, type, perm, icon, sort, visible, status, deleted)
VALUES (65, 56, '提交工单', '/ticket/create', 'ticket/create', 'M', NULL, 'flag', 0, 1, 1, 0);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 65), (2, 65);
