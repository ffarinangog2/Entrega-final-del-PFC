import { expect, test, type Locator, type Page } from '@playwright/test'

const docenteUsuario = process.env.DEMO_DOCENTE_USERNAME
const docentePassword = process.env.DEMO_DOCENTE_PASSWORD
const adminUsuario = process.env.DEMO_ADMIN_PISO_USERNAME
const adminPassword = process.env.DEMO_ADMIN_PISO_PASSWORD

function fechaE2e(offset: number) {
  return new Date(Date.now() + (180 + offset) * 86_400_000).toISOString().slice(0, 10)
}

async function login(page: Page, username: string, password: string) {
  await page.goto('http://localhost:3000/login')
  await page.evaluate(() => sessionStorage.clear())
  await page.reload()
  await page.getByLabel('Usuario o correo').fill(username)
  await page.getByLabel(/Contrase/).fill(password)
  const response = page.waitForResponse((r) =>
    r.url().includes('/api/v1/auth/login') && r.request().method() === 'POST')
  await page.getByRole('button', { name: /Iniciar sesi/ }).click()
  const loginResponse = await response
  expect(loginResponse.status(), `Login real mediante ${loginResponse.url()}`).toBe(200)
  await expect(page).toHaveURL(/\/main$/, { timeout: 30_000 })
}

async function abrirFormulario(page: Page) {
  const esGet = (ruta: string) => (response: { request(): { method(): string }; url(): string }) =>
    response.request().method() === 'GET' && new URL(response.url()).pathname === ruta
  const respuestas = [
    page.waitForResponse((r) => r.request().method() === 'GET' && new URL(r.url()).pathname.startsWith('/api/v1/docentes/perfil/')),
    page.waitForResponse((r) => r.request().method() === 'GET' && new URL(r.url()).pathname.startsWith('/api/v1/horarios/docente/')),
    page.waitForResponse(esGet('/api/v1/materias')),
    page.waitForResponse(esGet('/api/v1/periodos-lectivos/actual')),
    page.waitForResponse(esGet('/api/v1/laboratorios')),
  ]
  await page.getByRole('link', { name: /Nueva solicitud/ }).click()
  for (const response of await Promise.all(respuestas)) expect(response.status()).toBe(200)
  await expect(page.getByLabel('Docente')).toBeDisabled()
}

async function seleccionarPorTexto(select: Locator, texto: string) {
  const value = await select.locator('option').evaluateAll((options, fragmento) =>
    options.find((option) => option.textContent?.includes(fragmento as string))?.getAttribute('value'), texto)
  expect(value, `Debe existir una opción que contenga ${texto}`).toBeTruthy()
  await select.selectOption(value!)
}

async function crearSolicitud(page: Page, fecha: string, motivo: string) {
  await page.getByLabel('Materia').selectOption({ index: 1 })
  await seleccionarPorTexto(page.getByLabel('Laboratorio'), 'DEMO-LAB-A')
  await page.getByLabel('Fecha').fill(fecha)
  await page.getByLabel('Hora inicio').fill('18:00')
  await page.getByLabel('Hora fin').fill('19:00')
  await page.getByLabel('Motivo').fill(motivo)
  const disponibilidad = page.waitForResponse((r) =>
    r.request().method() === 'GET' && r.url().includes('/api/v1/disponibilidad/laboratorios/'))
  await page.getByRole('button', { name: 'Comprobar disponibilidad' }).click()
  expect((await disponibilidad).status()).toBe(200)
  await expect(page.getByRole('status')).toHaveText('Disponible')
  const creacion = page.waitForResponse((r) => r.url().endsWith('/api/v1/solicitudes') && r.request().method() === 'POST')
  await page.getByRole('button', { name: 'Crear solicitud' }).click()
  expect((await creacion).status()).toBe(201)
  await expect(page).toHaveURL(/\/solicitudes\/[0-9a-f-]+$/)
}

test.describe('Flujo Freddy integrado', () => {
  test.describe.configure({ retries: 0, timeout: 75_000 })
  test.skip(!docenteUsuario || !docentePassword || !adminUsuario || !adminPassword,
    'Credenciales demo no configuradas')

  test('DOCENTE crea, consulta y cancela sin UUID manuales', async ({ page }) => {
    await login(page, docenteUsuario!, docentePassword!)
    await abrirFormulario(page)
    await expect(page.getByLabel('Docente ID')).toHaveCount(0)
    await expect(page.getByLabel('Materia ID')).toHaveCount(0)
    await expect(page.getByLabel('Periodo Lectivo ID')).toHaveCount(0)
    await expect(page.getByLabel('Materia').locator('option')).toHaveCount(3)
    await expect(page.getByLabel('Laboratorio').locator('option')).toHaveCount(3)
    await crearSolicitud(page, fechaE2e(0), `E2E docente ${crypto.randomUUID()}`)
    await expect(page.locator('.reserva-card__status', { hasText: 'PENDIENTE' })).toBeVisible()
    await page.getByLabel('Comentario').fill('Retiro controlado E2E')
    page.once('dialog', (dialog) => dialog.accept())
    const cancelacion = page.waitForResponse((r) => r.request().method() === 'POST' && r.url().endsWith('/cancelar'))
    await page.getByRole('button', { name: 'Cancelar/Retirar' }).click()
    expect((await cancelacion).status()).toBe(200)
    await expect(page.locator('.reserva-card__status', { hasText: 'CANCELADA' })).toBeVisible()
  })

  test('ADMINISTRADOR_PISO revisa y propone solo dentro de su piso', async ({ page }) => {
    await login(page, docenteUsuario!, docentePassword!)
    await abrirFormulario(page)
    await crearSolicitud(page, fechaE2e(1), `E2E gestor ${crypto.randomUUID()}`)
    const solicitudUrl = page.url()
    await login(page, adminUsuario!, adminPassword!)
    await page.goto(solicitudUrl)
    await expect(page.locator('.reserva-card__status', { hasText: 'PENDIENTE' })).toBeVisible()
    const revision = page.waitForResponse((r) => r.request().method() === 'POST' && r.url().endsWith('/revision'))
    await page.getByRole('button', { name: /Poner en revisi/ }).click()
    expect((await revision).status()).toBe(200)
    await expect(page.locator('.reserva-card__status', { hasText: 'EN REVISION' })).toBeVisible()

    const propuestaForm = page.getByRole('heading', { name: 'Proponer alternativa' }).locator('..')
    await seleccionarPorTexto(propuestaForm.getByLabel('Laboratorio'), 'DEMO-LAB-B')
    await propuestaForm.getByLabel(/Observaci/).fill('Fuera de piso E2E')
    const fueraPiso = page.waitForResponse((r) => r.request().method() === 'POST' && r.url().endsWith('/propuesta'))
    await page.getByRole('button', { name: 'Enviar propuesta' }).click()
    expect((await fueraPiso).status()).toBe(403)

    await seleccionarPorTexto(propuestaForm.getByLabel('Laboratorio'), 'DEMO-LAB-A')
    await propuestaForm.getByLabel('Hora inicio').fill('19:00')
    await propuestaForm.getByLabel('Hora fin').fill('20:00')
    await propuestaForm.getByLabel(/Observaci/).fill('Alternativa válida E2E')
    const propuesta = page.waitForResponse((r) => r.request().method() === 'POST' && r.url().endsWith('/propuesta'))
    await page.getByRole('button', { name: 'Enviar propuesta' }).click()
    expect((await propuesta).status()).toBe(200)
    await expect(page.locator('.reserva-card__status', { hasText: 'PROPUESTA' })).toBeVisible()
    await page.getByRole('link', { name: /Reservas/ }).click()
    await expect(page.getByText('DEMO-LAB-B')).toHaveCount(0)
  })
})
