import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthApiError, forgotPassword, login, refresh, resetPassword } from './authApi'

describe('authApi', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('envía los cuatro comandos con JSON', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true, json: vi.fn().mockResolvedValue({ message: 'ok' }),
    })
    vi.stubGlobal('fetch', fetchMock)
    await login('user', 'pass')
    await refresh('refresh-token')
    await forgotPassword('user@example.test')
    await resetPassword('token', 'new', 'new')
    expect(fetchMock).toHaveBeenCalledTimes(4)
    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/auth/login')
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({ username: 'user', password: 'pass' })
    expect(fetchMock.mock.calls[3][0]).toBe('/api/v1/auth/reset-password')
  })

  it('conserva el mensaje del backend en errores HTTP', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false, status: 401, json: vi.fn().mockResolvedValue({ message: 'Credenciales inválidas' }),
    }))
    await expect(login('user', 'bad')).rejects.toEqual(
      expect.objectContaining({ name: 'AuthApiError', status: 401, message: 'Credenciales inválidas' }),
    )
  })

  it('usa mensajes seguros ante body inválido o red no disponible', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false, status: 500, json: vi.fn().mockRejectedValue(new Error('invalid')),
    }))
    await expect(forgotPassword('user')).rejects.toBeInstanceOf(AuthApiError)
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')))
    await expect(refresh('token')).rejects.toEqual(expect.objectContaining({ status: 503 }))
  })
})
