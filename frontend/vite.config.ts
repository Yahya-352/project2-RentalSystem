import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// The Spring Boot backend has no CORS configuration, so in development every API call goes
// through Vite's proxy: the browser talks to /api on the same origin, and Vite forwards the
// request to the backend with the /api prefix stripped.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '');
  const backend = env.BACKEND_URL || 'http://localhost:8080';

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: backend,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
        },
      },
    },
    preview: {
      port: 4173,
      proxy: {
        '/api': {
          target: backend,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
        },
      },
    },
  };
});
