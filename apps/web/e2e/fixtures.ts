import { expect, test as base } from '@playwright/test'

export { expect }

export const test = base.extend({
  page: async ({ page }, runTest) => {
    const criticalErrors: string[] = []
    page.on('pageerror', (error) => criticalErrors.push(`pageerror: ${error.message}`))
    page.on('console', (message) => {
      const text = message.text()
      const browserHttpNoise = text.startsWith('Failed to load resource:')
      if (message.type() === 'error' && !browserHttpNoise) {
        criticalErrors.push(`console.error: ${text}`)
      }
    })
    page.on('response', (response) => {
      if (response.status() >= 500) {
        criticalErrors.push(`HTTP ${response.status()}: ${response.url()}`)
      }
    })
    await runTest(page)
    expect(criticalErrors, 'La página no debe emitir errores críticos').toEqual([])
  },
})
