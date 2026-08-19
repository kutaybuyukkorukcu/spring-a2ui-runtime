import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/a2ui': {
        target: 'http://localhost:5001',
        changeOrigin: true,
      },
      '/api': {
        target: 'http://localhost:5001',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      '@a2ui/react-v0_9-css': fileURLToPath(
        new URL('./node_modules/@a2ui/react/v0_9/index.css', import.meta.url),
      ),
    },
  },
  optimizeDeps: {
    include: ['@a2ui/react/v0_9', '@a2ui/web_core/v0_9'],
  },
})
