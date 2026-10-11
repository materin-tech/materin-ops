import { requestClient } from '#/api/request';

export interface ResourceItem {
  contentType?: null | string;
  createdAt?: string;
  id: number;
  originalName: string;
  size: number;
  storageKey: string;
  storageType: string;
  uploaderId?: null | number;
}

export async function pageResources(params: Record<string, any>) {
  return requestClient.get<{ list: ResourceItem[]; total: number }>('/resource/files', { params });
}

export async function uploadResource(file: File) {
  const form = new FormData();
  form.append('file', file);
  return requestClient.post<ResourceItem>('/resource/files/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export function downloadUrl(id: number) {
  return `/api/resource/files/${id}/download`;
}

export async function deleteResource(id: number) {
  return requestClient.delete(`/resource/files/${id}`);
}
