import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/api/, ''),
        target: 'http://localhost:80',
      },
    },
  },
});
