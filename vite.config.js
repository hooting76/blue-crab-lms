import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
    plugins: [
      react(),
      VitePWA({
        registerType: 'autoUpdate',
        // 개발 환경에서도 PWA 기능 테스트
        devOptions: {
          enabled: true
        },        
        manifest: {
          ID: 'Blue-Crab',
          name: 'Blue-Crab LMS',
          short_name: 'Blue-Crab',
          description: 'Blue-Crab Academy LMS App',
          scope: '/',
          start_url: '/',
          icons: [
            {
              src: './public/favicon/android-icon-192x192.png',
              sizes: '192x192',
              type: 'image/png',
            },
          ],
          orientation: "any",
          display: 'standalone',
          background_color: '#ffffff',
          theme_color: '#ffffff',
          categories: ['education','books'],
          shortcuts:[
            {
              name:'Home',
              short_name: 'Home',
              description: '로그인페이지',
              url:'/',
              icons:[{src: './public/favicon/android-icon-192x192.png', sizes: '192x192'}]
            },
            {
              name:'Admin',
              short_name: 'Admin',
              description: '관리자페이지',
              url:'/admin',
              icons:[{src: './public/favicon/android-icon-192x192.png', sizes: '192x192'}]
            }
          ],          
        }, // menifest end

        workbox: {
          globPatterns: ['**/*.{js,css,html,png,svg,ico,jpg,jpeg,gif,json,webp}'],

          runtimeCaching: [
            {// 이미지 캐싱
              urlPattern: /\.(?:png|jpg|jpeg|svg|gif|webp)$/,
              handler: 'CacheFirst',
              options: {
                cacheName: 'image-cache',
                expiration: {
                  maxEntries: 60,
                  maxAgeSeconds: 30 * 24 * 60 * 60
                }
              }
            }// 이미지 캐싱 end
          ], // runtimeCaching end

          navigateFallback: '/index.html',
          maximumFileSizeToCacheInBytes: 5000000,
        }
      })
    ],

    build: {
      sourcemap: true,
      chunkSizeWarningLimit: 10000,
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



