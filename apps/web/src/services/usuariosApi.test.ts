import { afterEach, describe, expect, it, vi } from 'vitest'
import { crearPerfil, listarPerfiles, UsuariosApiError } from './usuariosApi'

describe('usuariosApi', () => {
  afterEach(() => { vi.unstubAllGlobals(); sessionStorage.clear() })

  it('lista y crea perfiles incluyendo token y JSON', async () => {
    sessionStorage.setItem('accessToken', 'token')
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: vi.fn().mockResolvedValue({ content: [{ id: '1' }] }) })
      .mockResolvedValueOnce({ ok: true, json: vi.fn().mockResolvedValue({ id: '2' }) })
    vi.stubGlobal('fetch', fetchMock)
    await expect(listarPerfiles()).resolves.toEqual([{ id: '1' }])
    await crearPerfil({ identificacion: '1', nombres: 'A', apellidos: 'B', emailInstitucional: 'a@test', emailPersonal: '', telefono: '', direccion: '', fechaNacimiento: '' })
    expect(fetchMock.mock.calls[0][1].headers).toEqual({ Authorization: 'Bearer token' })
    expect(fetchMock.mock.calls[1][1]).toMatchObject({ method: 'POST' })
    expect(fetchMock.mock.calls[1][1].headers).toMatchObject({ 'Content-Type': 'application/json', Authorization: 'Bearer token' })
  })

  it('expone error HTTP del backend y fallo de red', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false, status: 409, json: vi.fn().mockResolvedValue({ message: 'Duplicado' }),
    }))
    await expect(listarPerfiles()).rejects.toEqual(expect.objectContaining({ status: 409, message: 'Duplicado' }))
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')))
    await expect(listarPerfiles()).rejects.toBeInstanceOf(UsuariosApiError)
  })
})
