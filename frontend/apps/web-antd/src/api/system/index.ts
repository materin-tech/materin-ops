import { requestClient } from '#/api/request';

export interface SystemUser {
  avatar?: null | string;
  email?: null | string;
  id: number;
  nickname?: null | string;
  phone?: null | string;
  roleIds: number[];
  roleNames?: string[];
  status: number;
  username: string;
}

export interface SystemRole {
  code: string;
  id: number;
  menuIds: number[];
  name: string;
  remark?: null | string;
  sort: number;
  status: number;
}

export interface MenuNode {
  children: MenuNode[];
  component?: null | string;
  icon?: null | string;
  id: number;
  name: string;
  parentId: number;
  path?: null | string;
  perm?: null | string;
  sort?: number;
  status?: number;
  type: string;
  visible?: number;
}

export interface PageResult<T> {
  list: T[];
  page: number;
  pageSize: number;
  total: number;
}

// ---------- 用户 ----------

export async function pageUsers(params: Record<string, any>) {
  return requestClient.get<PageResult<SystemUser>>('/system/users', { params });
}

export async function createUser(data: Record<string, any>) {
  return requestClient.post('/system/users', data);
}

export async function updateUser(id: number, data: Record<string, any>) {
  return requestClient.put(`/system/users/${id}`, data);
}

export async function resetUserPassword(id: number, newPassword: string) {
  return requestClient.put(`/system/users/${id}/password`, { newPassword });
}

export async function changeUserStatus(id: number, status: number) {
  return requestClient.put(`/system/users/${id}/status/${status}`);
}

export async function deleteUser(id: number) {
  return requestClient.delete(`/system/users/${id}`);
}

// ---------- 角色 ----------

export async function pageRoles(params: Record<string, any>) {
  return requestClient.get<PageResult<SystemRole>>('/system/roles', { params });
}

export async function allRoles() {
  return requestClient.get<SystemRole[]>('/system/roles/all');
}

export async function createRole(data: Record<string, any>) {
  return requestClient.post('/system/roles', data);
}

export async function updateRole(id: number, data: Record<string, any>) {
  return requestClient.put(`/system/roles/${id}`, data);
}

export async function assignRoleMenus(id: number, menuIds: number[]) {
  return requestClient.put(`/system/roles/${id}/menus`, menuIds);
}

export async function deleteRole(id: number) {
  return requestClient.delete(`/system/roles/${id}`);
}

// ---------- 菜单 ----------

export async function menuTree() {
  return requestClient.get<MenuNode[]>('/system/menus/tree');
}

export async function menuList() {
  return requestClient.get<MenuNode[]>('/system/menus');
}

export async function createMenu(data: Record<string, any>) {
  return requestClient.post<number>('/system/menus', data);
}

export async function updateMenu(id: number, data: Record<string, any>) {
  return requestClient.put(`/system/menus/${id}`, data);
}

export async function deleteMenu(id: number) {
  return requestClient.delete(`/system/menus/${id}`);
}
