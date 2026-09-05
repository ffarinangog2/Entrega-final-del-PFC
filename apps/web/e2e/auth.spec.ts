import { expect, test } from './fixtures'
import type { Page } from '@playwright/test'

const username = 'admin'
const password = 'Admin123!'

async function login(page: Page) {
  await page.goto('/login')
  await page.getByLabel('Usuario o correo').fill(username)
  await page.getByLabel('Contraseña').fill(password)
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
}

test.describe('Autenticación web', () => {
  test('permite iniciar sesión con credenciales válidas', async ({ page }) => {
    await login(page)

    await expect(page).toHaveURL(/\/main$/)
    await expect(page.getByRole('navigation', { name: 'Navegación principal' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Cerrar sesión' })).toBeVisible()
  })

  test('muestra un error con credenciales incorrectas', async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel('Usuario o correo').fill(username)
    await page.getByLabel('Contraseña').fill('contraseña-incorrecta')
    await page.getByRole('button', { name: 'Iniciar sesión' }).click()

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('alert')).toContainText('Credenciales inválidas.')
  })

  test('redirige al login al acceder a una ruta protegida sin sesión', async ({ page }) => {
    await page.goto('/login')
    await page.evaluate(() => sessionStorage.clear())
    await page.goto('/main')

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: 'SCLI' })).toBeVisible()
  })

  test('cierra la sesión y elimina sus datos almacenados', async ({ page }) => {
    await login(page)
    await expect(page).toHaveURL(/\/main$/)

    await page.getByRole('button', { name: 'Cerrar sesión' }).click()

    await expect(page).toHaveURL(/\/login$/)
    await expect
      .poll(() => page.evaluate(() => sessionStorage.length))
      .toBe(0)
  })

  test('mantiene la sesión válida después de recargar la página', async ({ page }) => {
    await login(page)
    await expect(page).toHaveURL(/\/main$/)

    await page.reload()

    await expect(page).toHaveURL(/\/main$/)
    await expect(page.getByRole('navigation', { name: 'Navegación principal' })).toBeVisible()
  })
})
