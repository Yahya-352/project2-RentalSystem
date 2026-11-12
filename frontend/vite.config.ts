import { defineConfig, loadEnv, type ProxyOptions } from 'vite';
import react from '@vitejs/plugin-react';

// The backend is split into microservices and has no CORS configuration, so in development every
// API call goes through Vite's proxy: the browser talks to /api on the same origin, and Vite
// forwards the request to the service that owns the path, with the /api prefix stripped.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '');

  // First path segment after /api → the service that owns it.
  const services: Record<string, string> = {
    'users|profiles': env.USER_SERVICE_URL || 'http://localhost:8081',
    'cars|categories|makes|countries|cities|images': env.CAR_SERVICE_URL || 'http://localhost:8082',
    bookings: env.BOOKING_SERVICE_URL || 'http://localhost:8083',
    pay: env.PAYMENT_SERVICE_URL || 'http://localhost:8084',
    notifications: env.NOTIFICATION_SERVICE_URL || 'http://localhost:8085',
  };

  const proxy: Record<string, ProxyOptions> = {};
  for (const [paths, target] of Object.entries(services)) {
    proxy[`^/api/(${paths})(/|\\?|$)`] = {
      target,
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, ''),
    };
  }

  return {
    plugins: [react()],
    server: { port: 5173, proxy },
    preview: { port: 4173, proxy },
  };
});
