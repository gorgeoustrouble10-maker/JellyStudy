import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 默认走 API Gateway（8080）；若未启 Gateway，可设环境变量 VITE_DIRECT_BACKEND=true 直连三服务
const useGateway = process.env.VITE_DIRECT_BACKEND !== 'true'

export default defineConfig({
  plugins: [vue()],
  build: {
    target: 'esnext',
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (id.includes('echarts')) return 'vendor-echarts'
          if (id.includes('lucide-vue-next')) return 'vendor-icons'
          if (id.includes('axios')) return 'vendor-http'
          return 'vendor'
        }
      }
    }
  },
  optimizeDeps: {
    esbuildOptions: {
      target: 'esnext'
    }
  },
  server: {
    host: '127.0.0.1',
    port: 9945,
    proxy: useGateway
      ? {
          '/api': {
            target: 'http://127.0.0.1:8080',
            changeOrigin: true
          }
        }
      : {
          '/api/knowledge-points': { target: 'http://127.0.0.1:8081', changeOrigin: true },
          '/api/evaluations': { target: 'http://127.0.0.1:8083', changeOrigin: true },
          '/api/questions': { target: 'http://127.0.0.1:8082', changeOrigin: true },
          '/api/answers': { target: 'http://127.0.0.1:8082', changeOrigin: true },
          '/api/health/knowledge': {
            target: 'http://127.0.0.1:8081',
            changeOrigin: true,
            rewrite: () => '/api/health'
          },
          '/api/health/qa': {
            target: 'http://127.0.0.1:8082',
            changeOrigin: true,
            rewrite: () => '/api/health'
          },
          '/api/health/evaluate': {
            target: 'http://127.0.0.1:8083',
            changeOrigin: true,
            rewrite: () => '/api/health'
          },
          '/api/health/coach': {
            target: 'http://127.0.0.1:8084',
            changeOrigin: true,
            rewrite: () => '/api/health'
          },
          '/api/coach': { target: 'http://127.0.0.1:8084', changeOrigin: true },
          '/api/auth': { target: 'http://127.0.0.1:8084', changeOrigin: true }
        }
  }
})
