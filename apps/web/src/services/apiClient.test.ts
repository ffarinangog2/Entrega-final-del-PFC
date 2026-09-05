import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiRequest, configureUnauthorizedHandler } from './apiClient'

describe('apiClient central', () => {
  beforeEach(() => { sessionStorage.clear(); vi.stubGlobal('fetch', vi.fn()) })
  afterEach(() => { configureUnauthorizedHandler(null); vi.unstubAllGlobals() })

  it('agrega Bearer', async () => {
    sessionStorage.setItem('accessToken', 'access-1')
    vi.mocked(fetch).mockResolvedValue(new Response(JSON.stringify({ ok: true }), { status: 200 }))
    await apiRequest('/api/v1/reservas')
    expect(vi.mocked(fetch).mock.calls[0][1]?.headers).toMatchObject({ Authorization: 'Bearer access-1' })
  })

  it('refresca y reintenta una sola vez ante 401', async () => {
    sessionStorage.setItem('accessToken', 'expired')
    const refresh = vi.fn(async () => { sessionStorage.setItem('accessToken', 'fresh'); return true })
    configureUnauthorizedHandler(refresh)
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 401 })).mockResolvedValueOnce(new Response(JSON.stringify({ ok: true }), { status: 200 }))
    await expect(apiRequest<{ ok: boolean }>('/api/v1/reservas')).resolves.toEqual({ ok: true })
    expect(refresh).toHaveBeenCalledTimes(1)
    expect(fetch).toHaveBeenCalledTimes(2)
    expect(vi.mocked(fetch).mock.calls[1][1]?.headers).toMatchObject({ Authorization: 'Bearer fresh' })
  })

  it('un 403 no intenta refresh', async () => {
    const refresh = vi.fn(async () => true); configureUnauthorizedHandler(refresh)
    vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 403 }))
    await expect(apiRequest('/api/v1/reservas')).rejects.toMatchObject({ status: 403 })
    expect(refresh).not.toHaveBeenCalled(); expect(fetch).toHaveBeenCalledTimes(1)
  })

  it.each([
    [404, { message: 'No encontrado' }, 'No encontrado'],
    [404, {}, 'El recurso solicitado no existe.'],
    [409, { message: 'Conflicto custom' }, 'Conflicto custom'],
    [409, {}, 'Existe un conflicto de disponibilidad o estado.'],
    [423, { message: 'Bloqueado por intentos' }, 'Bloqueado por intentos'],
    [423, {}, 'La cuenta está bloqueada temporalmente.'],
    [500, { message: 'detalle interno' }, 'Se produjo un error al procesar la solicitud.'],
    [400, {}, 'No se pudo completar la solicitud.'],
  ])('traduce el error HTTP %s de forma segura', async (status, body, message) => {
    vi.mocked(fetch).mockResolvedValue(new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    }))
    await expect(apiRequest('/api/error')).rejects.toMatchObject({ status, message })
  })

  it('tolera errores sin body JSON y respuestas 204', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response('no-json', { status: 404 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
    await expect(apiRequest('/missing')).rejects.toMatchObject({
      message: 'El recurso solicitado no existe.',
    })
    await expect(apiRequest('/empty')).resolves.toBeUndefined()
  })

  it('reporta indisponibilidad en el primer intento y después de refresh', async () => {
    vi.mocked(fetch).mockRejectedValueOnce(new Error('offline'))
    await expect(apiRequest('/offline')).rejects.toMatchObject({ status: 503 })

    configureUnauthorizedHandler(vi.fn(async () => true))
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockRejectedValueOnce(new Error('offline again'))
    await expect(apiRequest('/retry-offline')).rejects.toMatchObject({ status: 503 })
  })

  it('conserva el 401 cuando el refresh no recupera la sesión', async () => {
    configureUnauthorizedHandler(vi.fn(async () => false))
    vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 401 }))
    await expect(apiRequest('/expired')).rejects.toMatchObject({ status: 401 })
    expect(fetch).toHaveBeenCalledTimes(1)
  })
})
