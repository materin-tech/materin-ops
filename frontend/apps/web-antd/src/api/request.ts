/**
 * 该文件可自行根据业务逻辑进行调整
 */
import type { RequestClientOptions } from '@vben/request';

import { useAppConfig } from '@vben/hooks';
import { preferences } from '@vben/preferences';
import {
  authenticateResponseInterceptor,
  defaultResponseInterceptor,
  errorMessageResponseInterceptor,
  RequestClient,
} from '@vben/request';
import { useAccessStore } from '@vben/stores';

import { message } from 'ant-design-vue';

import { useAuthStore } from '#/store';

import { refreshTokenApi } from './core';

const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);

function createRequestClient(baseURL: string, options?: RequestClientOptions) {
  const client = new RequestClient({
    ...options,
    baseURL,
  });

  /**
   * 重新认证逻辑
   */
  async function doReAuthenticate() {
    console.warn('Access token or refresh token is invalid or expired. ');
    const accessStore = useAccessStore();
    const authStore = useAuthStore();
    accessStore.setAccessToken(null);
    if (
      preferences.app.loginExpiredMode === 'modal' &&
      accessStore.isAccessChecked
    ) {
      accessStore.setLoginExpired(true);
    } else {
      await authStore.logout();
    }
  }

  /**
   * 刷新token逻辑
   */
  async function doRefreshToken() {
    const accessStore = useAccessStore();
    const resp = await refreshTokenApi();
    const newToken = resp.data;
    accessStore.setAccessToken(newToken);
    return newToken;
  }

  function formatToken(token: null | string) {
    return token ? `Bearer ${token}` : null;
  }

  // 请求头处理
  client.addRequestInterceptor({
    fulfilled: async (config) => {
      const accessStore = useAccessStore();

      config.headers.Authorization = formatToken(accessStore.accessToken);
      config.headers['Accept-Language'] = preferences.app.locale;
      // 全链路追踪：每个请求携带 request-id，后端透传/生成 trace-id
      const requestId = crypto.randomUUID().replaceAll('-', '');
      config.headers['X-Trace-Id'] = requestId;
      (window as any).__lastTraceId = requestId;
      return config;
    },
  });

  // 处理返回的响应数据格式
  client.addResponseInterceptor(
    defaultResponseInterceptor({
      codeField: 'code',
      dataField: 'data',
      successCode: 0,
    }),
  );

  // 后端业务码 401（登录失效）：HTTP 状态恒为 200，vben 自带的
  // authenticateResponseInterceptor 只认 HTTP 401，永远不触发。
  // 此处按响应体 code=401 强制重置登录状态并跳转登录页。
  let reAuthenticating = false;
  client.addResponseInterceptor({
    rejected: async (error: any) => {
      const bodyCode = error?.response?.data?.code;
      const isAuthApi = error?.config?.url?.includes('/auth/login')
        || error?.config?.url?.includes('/auth/logout');
      if (bodyCode === 401 && !isAuthApi && !reAuthenticating) {
        reAuthenticating = true;
        try {
          const accessStore = useAccessStore();
          accessStore.setAccessToken(null);
          accessStore.setAccessCodes([]);
          await useAuthStore().logout();
        } finally {
          // 防止并发 401 重复触发
          setTimeout(() => {
            reAuthenticating = false;
          }, 1000);
        }
      }
      return Promise.reject(error);
    },
  });

  // token过期的处理
  client.addResponseInterceptor(
    authenticateResponseInterceptor({
      client,
      doReAuthenticate,
      doRefreshToken,
      enableRefreshToken: preferences.app.enableRefreshToken,
      formatToken,
    }),
  );

  // 记录后端返回的 traceId（与前端 request-id 一致或后端生成）
  client.addResponseInterceptor({
    fulfilled: (response) => {
      const traceId = response?.headers?.['x-trace-id'];
      if (traceId) (window as any).__lastTraceId = traceId;
      return response;
    },
  });

  // 通用的错误处理,如果没有进入上面的错误处理逻辑，就会进入这里
  client.addResponseInterceptor(
    errorMessageResponseInterceptor((msg: string, error) => {
      // 这里可以根据业务进行定制,你可以拿到 error 内的信息进行定制化处理，根据不同的 code 做不同的提示，而不是直接使用 message.error 提示 msg
      // 当前mock接口返回的错误字段是 error 或者 message
      const responseData = error?.response?.data ?? {};
      const errorMessage = responseData?.error ?? responseData?.message ?? '';
      // 如果没有错误信息，则会根据状态码进行提示
      message.error(errorMessage || msg);
    }),
  );

  return client;
}

export const requestClient = createRequestClient(apiURL, {
  responseReturn: 'data',
});

export const baseRequestClient = new RequestClient({ baseURL: apiURL });
