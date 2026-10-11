import axios from 'axios';

const request = axios.create({ baseURL: '/api', timeout: 15000 });

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('h5_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 业务码 401（登录失效）→ 清会话强制跳登录（与 PC 端同口径）
let reauth = false;
request.interceptors.response.use(
  (resp) => {
    const { code, data, message } = resp.data || {};
    if (code === 0) return data;
    if (code === 401) forceLogin();
    return Promise.reject(new Error(message || '请求失败'));
  },
  (error) => {
    const bodyCode = error?.response?.data?.code;
    if (bodyCode === 401) forceLogin();
    return Promise.reject(error);
  },
);

function forceLogin() {
  if (reauth) return;
  reauth = true;
  localStorage.removeItem('h5_token');
  localStorage.removeItem('h5_user');
  setTimeout(() => {
    window.location.hash = '#/login';
    window.location.reload();
    reauth = false;
  }, 300);
}

export const login = (data) => request.post('/auth/login', data);
export const logout = () => request.post('/auth/logout', {}).catch(() => {});
export const getUserInfo = () => request.get('/user/info');

// 工单
export const pageTickets = (params) => request.get('/ticket/tickets', { params });
export const ticketDetail = (id) => request.get(`/ticket/tickets/${id}`);
export const ticketActions = (id) => request.get(`/ticket/tickets/${id}/actions`);
export const createTicket = (data) => request.post('/ticket/tickets', data);
export const acceptTicket = (id) => request.put(`/ticket/tickets/${id}/accept`, {});
export const changeTicketStatus = (id, s) => request.put(`/ticket/tickets/${id}/status/${s}`);
export const closeTicket = (id) => request.put(`/ticket/tickets/${id}/close`);
export const commentTicket = (id, content) => request.put(`/ticket/tickets/${id}/comment`, { content });
export const delegateTicket = (id, userId) => request.put(`/ticket/tickets/${id}/delegate`, { userId });

// 产品
export const listProducts = (params) => request.get('/requirement/products', { params });
export const productDetail = (id) => request.get(`/requirement/products/${id}`);
export const createProduct = (data) => request.post('/requirement/products', data);
export const updateProduct = (id, data) => request.put(`/requirement/products/${id}`, data);
export const deleteProduct = (id) => request.delete(`/requirement/products/${id}`);
export const productModuleTree = (id) => request.get(`/requirement/products/${id}/modules`);

// 通知 / 任务 / 项目 / 用户
export const fetchMyNotices = (params) => request.get('/notice/my', { params });
export const markNoticeRead = (id) => request.put(`/notice/${id}/read`);
export const unreadCount = () => request.get('/notice/unread-count');
export const pageTasks = (params) => request.get('/requirement/tasks', { params });
export const projectOptions = () => request.get('/requirement/projects/options');
export const listUsers = () => request.get('/system/users', { params: { page: 1, pageSize: 500 } });
