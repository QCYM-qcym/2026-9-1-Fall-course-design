import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    include: ['src/components/management/**/*.spec.js'],
    setupFiles: ['src/components/management/testSetup.js'],
    maxWorkers: 2
  }
})
