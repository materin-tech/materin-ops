// H5 轻量埋点：PV + 点击，批量上报
let buffer = [];
let sessionId = localStorage.getItem('h5-track-sid') || (localStorage.setItem('h5-track-sid', crypto.randomUUID().replaceAll('-', '')), localStorage.getItem('h5-track-sid'));

export function track(event, extra = {}) {
  buffer.push({
    event,
    client: 'h5',
    page: location.hash,
    target: extra.target,
    props: extra.props ? JSON.stringify(extra.props) : undefined,
    sessionId,
  });
  if (buffer.length >= 10) flush();
}

let flushing = false;
export function flush() {
  if (buffer.length === 0 || flushing) return;
  flushing = true;
  const batch = buffer.splice(0, buffer.length);
  fetch('/api/track/report', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(batch),
    keepalive: true,
  }).catch(() => {}).finally(() => (flushing = false));
}

export function initTrack(router) {
  setInterval(flush, 10_000);
  window.addEventListener('beforeunload', flush);
  router.afterEach((to) => track('pv', { page: to.fullPath }));
  document.addEventListener('click', (e) => {
    const el = e.target?.closest?.('[data-track]');
    if (el) track('click', { target: el.dataset.track || el.textContent?.slice(0, 30), page: location.hash });
  }, true);
}
