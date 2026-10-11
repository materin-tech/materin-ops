/**
 * 轻量埋点：PV / 点击 / 自定义事件，批量缓冲上报（非侵入：main.ts 一行接入）
 */
import { requestClient } from '#/api/request';

interface TrackItem {
  client: string;
  event: string;
  page?: string;
  props?: string;
  sessionId?: string;
  target?: string;
  traceId?: string;
}

const SESSION_KEY = 'track-session-id';
const BUFFER_MAX = 20;
const FLUSH_INTERVAL = 10_000;

let buffer: TrackItem[] = [];
let timer: null | ReturnType<typeof setInterval> = null;

function sessionId(): string {
  let id = localStorage.getItem(SESSION_KEY);
  if (!id) {
    id = crypto.randomUUID().replaceAll('-', '');
    localStorage.setItem(SESSION_KEY, id);
  }
  return id;
}

function push(item: Omit<TrackItem, 'client' | 'sessionId'>) {
  buffer.push({ ...item, client: 'web', sessionId: sessionId() });
  if (buffer.length >= BUFFER_MAX) {
    flush();
  }
}

export function track(event: 'click' | 'custom' | 'pv', extra?: { page?: string; props?: Record<string, any>; target?: string }) {
  push({
    event,
    page: extra?.page ?? window.location.hash,
    target: extra?.target,
    props: extra?.props ? JSON.stringify(extra.props) : undefined,
    traceId: (window as any).__lastTraceId,
  });
}

export function flush() {
  if (buffer.length === 0) return;
  const batch = buffer.splice(0, buffer.length);
  try {
    navigator.sendBeacon?.('/api/track/report', new Blob([JSON.stringify(batch)], { type: 'application/json' }))
      ?? requestClient.post('/track/report', batch);
  } catch {
    // 埋点失败静默
  }
}

/** 全局接入：路由 PV + 带 data-track 的点击自动采集（一行调用） */
export function initTrack(router: { afterEach: (fn: (to: any) => void) => void }) {
  if (timer) return;
  timer = setInterval(flush, FLUSH_INTERVAL);
  window.addEventListener('beforeunload', flush);

  router.afterEach((to) => {
    track('pv', { page: to.fullPath });
  });

  document.addEventListener(
    'click',
    (e) => {
      const el = (e.target as HTMLElement)?.closest?.('[data-track]') as HTMLElement | null;
      if (el) {
        track('click', {
          target: el.dataset.track || el.textContent?.slice(0, 30),
          page: window.location.hash,
        });
      }
    },
    true,
  );
}
