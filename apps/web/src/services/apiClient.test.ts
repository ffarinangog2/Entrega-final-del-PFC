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
    expect(refresh).toHaveBeenCalledTimes(1); expect(fetch).toHaveBeenCalledTimes(2)
    expect(vi.mocked(fetch).mock.calls[1][1]?.headers).toMatchObject({ Authorization: 'Bearer fresh' })
  })

  it('un 403 no intenta refresh', async () => {
    const refresh = vi.fn(async () => true); configureUnauthorizedHandler(refresh)
    vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 403 }))
    await expect(apiRequest('/api/v1/reservas')).rejects.toMatchObject({ status: 403 })
    expect(refresh).not.toHaveBeenCalled(); expect(fetch).toHaveBeenCalledTimes(1)
  })
})
