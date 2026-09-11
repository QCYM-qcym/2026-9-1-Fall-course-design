import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    include: ['src/components/management/**/*.spec.js', 'src/components/weather/**/*.spec.js', 'src/auth/**/*.spec.js'],
    setupFiles: ['src/components/management/testSetup.js'],
    maxWorkers: 2
  }
})
