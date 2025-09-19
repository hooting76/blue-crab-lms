import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
    plugins: [
      react(),
      VitePWA({
        registerType: 'autoUpdate',
        manifest: {
          ID: 'Blue-Crab',
          name: 'Blue-Crab LMS',
          short_name: 'Blue-Crab',
          description: 'Blue-Crab Academy LMS App',
          scope: '/',
          start_url: '/',
          icons: [
            {
              src: './public/favicon/android-icon-192x192.png', // 앱 아이콘 경로
              sizes: '192x192',
              type: 'image/png',
            },
          ],
          display: 'standalone',
          background_color: '#ffffff',
          theme_color: '#ffffff',
        },
        workbox: {
          globPatterns: ['**/*.{js,css,html,png,svg,ico,jpg,jsx,json}'],
        },
      })
    ],

    build: {
      chunkSizeWarningLimit: 5000,
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



