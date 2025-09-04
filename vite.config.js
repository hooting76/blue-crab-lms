import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
    plugins: [react()],

    build: {
      chunkSizeWarningLimit: 5000, // kb 단위
    },

    define: {
      global: {},
    },

    server: {
        proxy: {
          '/api' : {
          target: 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ""),
        },
      },
    },
});



