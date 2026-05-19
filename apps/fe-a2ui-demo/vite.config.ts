import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/a2ui': {
        target: 'http://localhost:5001',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      '@a2ui/react/styles': '@a2ui/react/v0_8/styles/index.js',
    },
  },
  optimizeDeps: {
    include: ['@a2ui/react/v0_8', '@a2ui/web_core'],
  },
})