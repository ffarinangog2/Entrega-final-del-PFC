import { expect, test } from './fixtures'
import type { Page, TestInfo } from '@playwright/test'
import { mkdirSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

type Decision = {
  decision_id: string
  request: string
  endpoint: string
  method: string
  identity: string
  role: string
  expected_http: string
  observed_http: string
  assertion_pass: boolean
  attempt: number
}

const outputDir = process.env.E3_SECURITY_OUTPUT_DIR
const gateway = process.env.GATEWAY_BASE_URL?.replace(/\/$/, '')
const docente = process.env.DEMO_DOCENTE_USERNAME
const docentePassword = process.env.DEMO_DOCENTE_PASSWORD
const adminPiso = process.env.DEMO_ADMIN_PISO_USERNAME
const adminPisoPassword = process.env.DEMO_ADMIN_PISO_PASSWORD
const repetition = Number(process.env.E3_REPETITION)

function requireConfiguration() {
  for (const [name, value] of Object.entries({ outputDir, gateway, docente, docentePassword, adminPiso, adminPisoPassword })) {
    if (!value) throw new Error(`Falta configuración obligatoria: ${name}`)
  }
  if (![1, 2, 3].includes(repetition)) throw new Error('E3_REPETITION debe ser 1, 2 o 3')
}

function add(rows: Decision[], info: TestInfo, value: Omit<Decision, 'attempt' | 'assertion_pass'>) {
  rows.push({ ...value, attempt: info.retry, assertion_pass: value.expected_http === value.observed_http })
}

async function login(page: Page, username: string, password: string) {
  await page.goto('http://localhost:3000/login')
  await page.evaluate(() => sessionStorage.clear())
  await page.reload()
  await page.getByLabel('Usuario o correo').fill(username)
  await page.getByLabel(/Contrase/).fill(password)
  const response = page.waitForResponse((item) => item.url().includes('/api/v1/auth/login') && item.request().method() === 'POST')
  await page.getByRole('button', { name: /Iniciar sesi/ }).click()
  const observed = await response
  await expect(page).toHaveURL(/\/main$/, { timeout: 30_000 })
  return observed.status()
}

async function openForm(page: Page) {
  await page.getByRole('link', { name: 'Nueva solicitud', exact: true }).click()
  await expect(page.getByLabel('Docente')).toBeDisabled()
}

async function selectContaining(select: ReturnType<Page['getByLabel']>, fragment: string) {
  const value = await select.locator('option').evaluateAll((options, text) =>
    options.find((option) => option.textContent?.includes(text as string))?.getAttribute('value'), fragment)
  expect(value, `Debe existir una opción que contenga ${fragment}`).toBeTruthy()
  await select.selectOption(value!)
  return value!
}

function dateFor(maximum: string, offset: number) {
  const date = new Date(`${maximum}T00:00:00Z`)
  date.setUTCDate(date.getUTCDate() - ((3 - repetition) * 2 + 1 - offset))
  return date.toISOString().slice(0, 10)
}

async function createRequest(page: Page, offset: number) {
  await page.getByLabel('Materia').selectOption({ index: 1 })
  await selectContaining(page.getByLabel('Laboratorio'), 'DEMO-LAB-A')
  const date = page.getByLabel('Fecha')
  const maximum = await date.getAttribute('max')
  expect(maximum, 'Debe existir una fecha máxima para el período activo').toBeTruthy()
  await date.fill(dateFor(maximum!, offset))
  await page.getByLabel('Hora inicio').fill('18:00')
  await page.getByLabel('Hora fin').fill('19:00')
  await page.getByLabel('Motivo').fill(`E3 seguridad r${repetition} ${crypto.randomUUID()}`)
  await page.getByRole('button', { name: 'Comprobar disponibilidad' }).click()
  await expect(page.getByRole('status')).toHaveText('Disponible')
  const response = page.waitForResponse((item) => item.url().endsWith('/api/v1/solicitudes') && item.request().method() === 'POST')
  await page.getByRole('button', { name: 'Crear solicitud' }).click()
  const created = await response
  await expect(page).toHaveURL(/\/solicitudes\/[0-9a-f-]+$/)
  return created.status()
}

test.describe('Seguridad E3', () => {
  test('matriz predefinida de siete decisiones por Gateway', async ({ page, request }, testInfo) => {
    requireConfiguration()
    const rows: Decision[] = []
    try {
      const adminStatus = await login(page, 'admin', 'Admin123!')
      add(rows, testInfo, { decision_id: 'admin_login_permitido', request: 'login admin', endpoint: '/api/v1/auth/login', method: 'POST', identity: 'admin', role: 'ADMINISTRADOR', expected_http: '200', observed_http: String(adminStatus) })

      const invalid = await request.post(`${gateway}/api/v1/auth/login`, { data: { username: 'admin', password: 'contraseña-incorrecta' } })
      add(rows, testInfo, { decision_id: 'admin_login_invalido_401', request: 'login con credencial inválida', endpoint: '/api/v1/auth/login', method: 'POST', identity: 'admin', role: 'NO_AUTENTICADO', expected_http: '401', observed_http: String(invalid.status()) })

      const anonymous = await request.get(`${gateway}/api/v1/reservas`)
      add(rows, testInfo, { decision_id: 'reservas_sin_token_401', request: 'consulta sin token', endpoint: '/api/v1/reservas', method: 'GET', identity: 'anónimo', role: 'NO_AUTENTICADO', expected_http: '401', observed_http: String(anonymous.status()) })

      const docenteLogin = await login(page, docente!, docentePassword!)
      add(rows, testInfo, { decision_id: 'docente_login_permitido', request: 'login docente', endpoint: '/api/v1/auth/login', method: 'POST', identity: docente!, role: 'DOCENTE', expected_http: '200', observed_http: String(docenteLogin) })
      await openForm(page)
      const createStatus = await createRequest(page, 0)
      const detailUrl = page.url()
      const queryResponse = page.waitForResponse((item) => item.request().method() === 'GET' && item.url().includes('/api/v1/solicitudes/'))
      await page.reload()
      const queryStatus = (await queryResponse).status()
      await page.getByLabel('Comentario').fill('Cierre controlado E3')
      page.once('dialog', (dialog) => dialog.accept())
      const cancelResponse = page.waitForResponse((item) => item.request().method() === 'POST' && item.url().endsWith('/cancelar'))
      await page.getByRole('button', { name: 'Cancelar/Retirar' }).click()
      const cancelStatus = (await cancelResponse).status()
      add(rows, testInfo, { decision_id: 'docente_crea_consulta_cancela', request: 'crear/consultar/cancelar solicitud propia', endpoint: '/api/v1/solicitudes + /{id} + /cancelar', method: 'POST/GET/POST', identity: docente!, role: 'DOCENTE', expected_http: '201/200/200', observed_http: `${createStatus}/${queryStatus}/${cancelStatus}` })
      expect(page.url()).toBe(detailUrl)

      await login(page, docente!, docentePassword!)
      await openForm(page)
      const outsideLabId = await selectContaining(page.getByLabel('Laboratorio'), 'DEMO-LAB-B')
      await createRequest(page, 1)
      const requestUrl = page.url()
      await login(page, adminPiso!, adminPisoPassword!)
      await page.goto(requestUrl)
      const reviewResponse = page.waitForResponse((item) => item.request().method() === 'POST' && item.url().endsWith('/revision'))
      await page.getByRole('button', { name: /Poner en revisi/ }).click()
      const reviewStatus = (await reviewResponse).status()
      const proposalHeading = page.getByRole('heading', { name: 'Proponer alternativa' })
      await expect(proposalHeading).toBeVisible()
      const form = proposalHeading.locator('..')
      const proposalDate = await form.locator('input[type="date"]').inputValue()
      const deniedStatus = await page.evaluate(async ({ endpoint, laboratorioId, fecha }) => {
        const response = await fetch(endpoint, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${sessionStorage.getItem('accessToken')}` },
          body: JSON.stringify({
            laboratorioId,
            fecha,
            horaInicio: '18:00', horaFin: '19:00', observacion: 'Fuera de scope E3',
          }),
        })
        return response.status
      }, { endpoint: `${gateway}/api/v1/solicitudes/${requestUrl.split('/').pop()}/propuesta`, laboratorioId: outsideLabId, fecha: proposalDate })
      add(rows, testInfo, { decision_id: 'admin_piso_fuera_scope_403', request: 'propuesta fuera de piso', endpoint: '/api/v1/solicitudes/{id}/propuesta', method: 'POST', identity: adminPiso!, role: 'ADMINISTRADOR_PISO', expected_http: '403', observed_http: String(deniedStatus) })

      await selectContaining(form.getByLabel('Laboratorio'), 'DEMO-LAB-A')
      await form.getByLabel('Hora inicio').fill('19:00')
      await form.getByLabel('Hora fin').fill('20:00')
      await form.getByLabel(/Observaci/).fill('Dentro de scope E3')
      const allowedResponse = page.waitForResponse((item) => item.request().method() === 'POST' && item.url().endsWith('/propuesta'))
      await page.getByRole('button', { name: 'Enviar propuesta' }).click()
      const allowedStatus = (await allowedResponse).status()
      add(rows, testInfo, { decision_id: 'admin_piso_scope_permitido', request: 'revisión/propuesta dentro de piso', endpoint: '/api/v1/solicitudes/{id}/revision + /propuesta', method: 'POST/POST', identity: adminPiso!, role: 'ADMINISTRADOR_PISO', expected_http: '200/200', observed_http: `${reviewStatus}/${allowedStatus}` })

      // Restauración operativa: la solicitud auxiliar no queda ocupando la franja.
      await login(page, docente!, docentePassword!)
      await page.goto(requestUrl)
      await page.getByLabel('Comentario').fill('Limpieza controlada E3')
      page.once('dialog', (dialog) => dialog.accept())
      const cleanupResponse = page.waitForResponse((item) => item.request().method() === 'POST' && item.url().endsWith('/cancelar'))
      await page.getByRole('button', { name: 'Cancelar/Retirar' }).click()
      expect((await cleanupResponse).status(), 'La fixture debe volver a liberar la franja').toBe(200)

      for (const row of rows) expect(row.assertion_pass, `${row.decision_id}: ${row.observed_http}`).toBe(true)
    } finally {
      mkdirSync(outputDir!, { recursive: true })
      writeFileSync(join(outputDir!, `decisiones-attempt-${testInfo.retry}.json`), JSON.stringify(rows, null, 2) + '\n', 'utf8')
    }
  })
})
