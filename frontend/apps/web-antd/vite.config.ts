import { defineConfig } from '@vben/vite-config';

export default defineConfig(async () => {
  return {
    application: {},
    vite: {
      server: {
        proxy: {
          '/api': {
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/api/, ''),
            // mock代理目标地址
            // 联调后端（materin-ops backend）
            target: 'http://localhost:8080',
            ws: true,
          },
        },
      },
    },
  };
});
