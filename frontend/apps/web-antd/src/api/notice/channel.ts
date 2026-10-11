import { requestClient } from '#/api/request';

export interface NotifyChannel {
  enabled: number;
  id?: number;
  name: string;
  secret?: null | string;
  type: 'dingtalk' | 'feishu' | 'wecom';
  webhookUrl: string;
}

export async function listChannels() {
  return requestClient.get<NotifyChannel[]>('/notice/channels');
}

export async function saveChannel(data: NotifyChannel) {
  return requestClient.post('/notice/channels', data);
}

export async function deleteChannel(id: number) {
  return requestClient.delete(`/notice/channels/${id}`);
}

export async function testChannel(id: number) {
  return requestClient.post<string>(`/notice/channels/${id}/test`);
}
