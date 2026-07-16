import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      // 音乐模块直连 8082，开发阶段不依赖网关
      '/music': {
        target: 'http://localhost:8082',
        changeOrigin: true
      }
    }
  }
})
