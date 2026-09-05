import { expect, test } from './fixtures'
import type { Page } from '@playwright/test'

const username = 'admin'
const password = 'Admin123!'

async function login(page: Page) {
  await page.goto('/login')
  await page.getByLabel('Usuario o correo').fill(username)
  await page.getByLabel('Contraseña').fill(password)
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await expect(page).toHaveURL(/\/main$/)
}

test.describe('Configuración: idioma y tema', () => {
  test('cambia idioma y tema, y ambos persisten tras recargar', async ({ page }) => {
    await login(page)
    await page.getByRole('link', { name: /Configuración|Settings/ }).click()
    await expect(page).toHaveURL(/\/settings$/)

    await expect(page.getByRole('heading', { name: 'Configuración' })).toBeVisible()
    await page.locator('#settings-idioma').selectOption('en')
    await expect(page.getByRole('heading', { name: 'Settings' })).toBeVisible()

    await page.getByRole('button', { name: 'Dark' }).click()
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')

    await page.reload()

    await expect(page.getByRole('heading', { name: 'Settings' })).toBeVisible()
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
    await expect(page.getByRole('button', { name: 'Dark' })).toHaveAttribute('aria-pressed', 'true')
  })
})
