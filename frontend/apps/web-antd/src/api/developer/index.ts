import { requestClient } from '#/api/request';

export interface DevApp {
  appKey: string;
  appSecret: string;
  description?: null | string;
  id: number;
  name: string;
  status: number;
}

export interface DevApi {
  id: number;
  method: string;
  name?: null | string;
  path: string;
  source?: string;
  status: number;
  tag?: null | string;
}

export interface PageResult<T> {
  list: T[];
  page: number;
  pageSize: number;
  total: number;
}

export interface DevAuth {
  appId: number;
  apis: DevApi[];
  authorizedApiIds: number[];
}

// ---------- 应用管理 ----------

export async function pageApps(params: Record<string, any>) {
  return requestClient.get<PageResult<DevApp>>('/developer/apps', { params });
}

export async function createApp(data: Partial<DevApp>) {
  return requestClient.post<DevApp>('/developer/apps', data);
}

export async function updateApp(id: number, data: Partial<DevApp>) {
  return requestClient.put(`/developer/apps/${id}`, data);
}

export async function resetAppSecret(id: number) {
  return requestClient.put<DevApp>(`/developer/apps/${id}/secret`);
}

export async function deleteApp(id: number) {
  return requestClient.delete(`/developer/apps/${id}`);
}

// ---------- 接口管理 ----------

export async function pageApis(params: Record<string, any>) {
  return requestClient.get<PageResult<DevApi>>('/developer/apis', { params });
}

export async function syncApis() {
  return requestClient.post<{ created: number; updated: number }>('/developer/apis/sync');
}

export async function createApi(data: Partial<DevApi>) {
  return requestClient.post('/developer/apis', data);
}

export async function updateApi(id: number, data: Partial<DevApi>) {
  return requestClient.put(`/developer/apis/${id}`, data);
}

export async function deleteApi(id: number) {
  return requestClient.delete(`/developer/apis/${id}`);
}

// ---------- 授权管理 ----------

export async function appAuthDetail(appId: number) {
  return requestClient.get<DevAuth>(`/developer/auth/apps/${appId}/apis`);
}

export async function saveAppAuth(appId: number, apiIds: number[]) {
  return requestClient.put(`/developer/auth/apps/${appId}/apis`, apiIds);
}
