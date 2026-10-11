import { requestClient } from '#/api/request';

export type * from './types';

import type {
  Iteration,
  PageResult,
  Product,
  ProductMember,
  ProductModuleNode,
  Project,
  Story,
  Task,
} from './types';

// ---------- 产品 ----------

export async function listProducts(params?: { keyword?: string; status?: number }) {
  return requestClient.get<Product[]>('/requirement/products', { params });
}

export async function productOptions() {
  return requestClient.get<Product[]>('/requirement/products/options');
}

export async function createProduct(data: Partial<Product>) {
  return requestClient.post<number>('/requirement/products', data);
}

export async function updateProduct(id: number, data: Partial<Product>) {
  return requestClient.put(`/requirement/products/${id}`, data);
}

export async function deleteProduct(id: number) {
  return requestClient.delete(`/requirement/products/${id}`);
}

export async function productModuleTree(id: number) {
  return requestClient.get<ProductModuleNode[]>(`/requirement/products/${id}/modules`);
}

export async function createProductModule(id: number, data: Partial<ProductModuleNode>) {
  return requestClient.post(`/requirement/products/${id}/modules`, data);
}

export async function updateProductModule(moduleId: number, data: Partial<ProductModuleNode>) {
  return requestClient.put(`/requirement/products/modules/${moduleId}`, data);
}

export async function deleteProductModule(moduleId: number) {
  return requestClient.delete(`/requirement/products/modules/${moduleId}`);
}

export async function productMembers(id: number) {
  return requestClient.get<ProductMember[]>(`/requirement/products/${id}/members`);
}

export async function saveProductMembers(id: number, members: ProductMember[]) {
  return requestClient.put(`/requirement/products/${id}/members`, { members });
}

// ---------- 需求 ----------

export async function pageStories(params: Record<string, any>) {
  return requestClient.get<PageResult<Story>>('/requirement/stories', { params });
}

export async function storyDetail(id: number) {
  return requestClient.get<Story>(`/requirement/stories/${id}`);
}

export async function createStory(data: Partial<Story>) {
  return requestClient.post<number>('/requirement/stories', data);
}

export async function updateStory(id: number, data: Partial<Story>) {
  return requestClient.put(`/requirement/stories/${id}`, data);
}

export async function deleteStory(id: number) {
  return requestClient.delete(`/requirement/stories/${id}`);
}

export async function changeStoryStatus(id: number, status: number) {
  return requestClient.put(`/requirement/stories/${id}/status/${status}`);
}

// ---------- 项目 ----------

export async function pageProjects(params: Record<string, any>) {
  return requestClient.get<PageResult<Project>>('/requirement/projects', { params });
}

export async function projectOptions() {
  return requestClient.get<Project[]>('/requirement/projects/options');
}

export async function createProject(data: Partial<Project>) {
  return requestClient.post('/requirement/projects', data);
}

export async function updateProject(id: number, data: Partial<Project>) {
  return requestClient.put(`/requirement/projects/${id}`, data);
}

export async function deleteProject(id: number) {
  return requestClient.delete(`/requirement/projects/${id}`);
}

export async function projectProducts(id: number) {
  return requestClient.get<number[]>(`/requirement/projects/${id}/products`);
}

export async function bindProjectProducts(id: number, productIds: number[]) {
  return requestClient.put(`/requirement/projects/${id}/products`, productIds);
}

// ---------- 迭代 ----------

export async function pageIterations(params: Record<string, any>) {
  return requestClient.get<PageResult<Iteration>>('/requirement/iterations', { params });
}

export async function createIteration(data: Partial<Iteration>) {
  return requestClient.post<number>('/requirement/iterations', data);
}

export async function updateIteration(id: number, data: Partial<Iteration>) {
  return requestClient.put(`/requirement/iterations/${id}`, data);
}

export async function deleteIteration(id: number) {
  return requestClient.delete(`/requirement/iterations/${id}`);
}

export async function changeIterationStatus(id: number, status: number) {
  return requestClient.put(`/requirement/iterations/${id}/status/${status}`);
}

// ---------- 任务 ----------

export async function pageTasks(params: Record<string, any>) {
  return requestClient.get<PageResult<Task>>('/requirement/tasks', { params });
}

export async function tasksByStory(storyId: number) {
  return requestClient.get<Task[]>(`/requirement/tasks/by-story/${storyId}`);
}

export async function createTask(data: Partial<Task>) {
  return requestClient.post<number>('/requirement/tasks', data);
}

export async function updateTask(id: number, data: Partial<Task>) {
  return requestClient.put(`/requirement/tasks/${id}`, data);
}

export async function deleteTask(id: number) {
  return requestClient.delete(`/requirement/tasks/${id}`);
}

export async function changeTaskStatus(id: number, status: number) {
  return requestClient.put(`/requirement/tasks/${id}/status/${status}`);
}

export async function assignTask(id: number, userId: number) {
  return requestClient.put(`/requirement/tasks/${id}/assignee`, { userId });
}

// ---------- 系统用户（负责人/指派下拉用） ----------

export interface SystemUserOption {
  id: number;
  nickname?: null | string;
  username: string;
}

export async function listSystemUsers() {
  return requestClient.get<{ list: SystemUserOption[] }>('/system/users', {
    params: { page: 1, pageSize: 500 },
  });
}
