import { defineConfig, devices } from '@playwright/test'
import base from './playwright.config'

export default defineConfig({
  ...base,
  timeout: 60_000,
  testIgnore: [],
  testMatch: 'seguridad-e3.spec.ts',
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
