import { requestClient } from '#/api/request';

export interface NoticeItem {
  content?: null | string;
  createdAt?: string;
  id: number;
  level: string;
  link?: null | string;
  readAt?: null | string;
  readFlag: number;
  receiverId: number;
  senderId?: null | number;
  title: string;
  type: string;
}

export interface NoticePage {
  list: NoticeItem[];
  total: number;
  unreadCount: number;
}

export async function fetchMyNotices(params?: { page?: number; pageSize?: number; readFlag?: number }) {
  const [pageRes, unread] = await Promise.all([
    requestClient.get<{ list: NoticeItem[]; total: number }>('/notice/my', { params }),
    requestClient.get<number>('/notice/unread-count'),
  ]);
  return { ...pageRes, unreadCount: unread } as NoticePage;
}

export async function fetchUnreadCount() {
  return requestClient.get<number>('/notice/unread-count');
}

export async function markNoticeRead(id: number) {
  return requestClient.put(`/notice/${id}/read`);
}

export async function markAllNoticesRead() {
  return requestClient.put('/notice/read-all');
}

export async function removeNotice(id: number) {
  return requestClient.delete(`/notice/${id}`);
}

export async function sendNotice(data: {
  content?: string;
  level?: string;
  link?: string;
  title: string;
  type?: string;
  userIds: number[];
}) {
  return requestClient.post('/notice/send', data);
}
