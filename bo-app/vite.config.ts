import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '127.0.0.1', // IPv4: "localhost" may resolve to ::1, which can time out
    port: 5174,
    strictPort: true,
    proxy: {
      '/api': 'http://127.0.0.1:8080', // same origin in dev, so no CORS needed
    },
  },
})
