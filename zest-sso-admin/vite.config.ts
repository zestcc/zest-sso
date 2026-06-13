import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const isProd = mode === 'production'

  return {
    plugins: [vue()],
    base: isProd ? '/admin/' : '/',
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    server: {
      port: 5175,
      proxy: {
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:9000',
          changeOrigin: true,
        },
      },
    },
  }
})
