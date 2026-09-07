import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  crearPerfil,
  listarContextosEstudiantesMasivos,
  listarPerfiles,
  UsuariosApiError,
} from './usuariosApi'

describe('usuariosApi', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    sessionStorage.clear()
  })

  it('lista y crea perfiles incluyendo token y JSON', async () => {
    sessionStorage.setItem('accessToken', 'token')
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: vi.fn().mockResolvedValue({ content: [{ id: '1' }], totalPages: 1 }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: vi.fn().mockResolvedValue({ id: '2' }),
      })
    vi.stubGlobal('fetch', fetchMock)
    await expect(listarPerfiles()).resolves.toEqual([{ id: '1' }])
    await crearPerfil({
      identificacion: '1',
      nombres: 'A',
      apellidos: 'B',
      emailInstitucional: 'a@test',
      emailPersonal: '',
      telefono: '',
      direccion: '',
      fechaNacimiento: '',
    })
    expect(fetchMock.mock.calls[0][1].headers).toEqual({ Authorization: 'Bearer token' })
    expect(fetchMock.mock.calls[1][1]).toMatchObject({ method: 'POST' })
    expect(fetchMock.mock.calls[1][1].headers).toMatchObject({
      'Content-Type': 'application/json',
      Authorization: 'Bearer token',
    })
  })

  it('recupera perfiles de páginas posteriores cuando totalPages > 1 (perfiles >100)', async () => {
    sessionStorage.setItem('accessToken', 'token')
    const pagina1 = Array.from({ length: 100 }, (_, i) => ({ id: `perfil-pag1-${i + 1}` }))
    const pagina2 = Array.from({ length: 25 }, (_, i) => ({ id: `perfil-pag2-${i + 1}` }))

    const fetchMock = vi.fn().mockImplementation((url: string) => {
      if (url.includes('page=0')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: pagina1, totalPages: 2 }),
        })
      }
      if (url.includes('page=1')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: pagina2, totalPages: 2 }),
        })
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [], totalPages: 2 }),
      })
    })

    vi.stubGlobal('fetch', fetchMock)

    const resultado = await listarPerfiles('juan')
    expect(resultado).toHaveLength(125)
    expect(resultado[0].id).toBe('perfil-pag1-1')
    expect(resultado[99].id).toBe('perfil-pag1-100')
    expect(resultado[100].id).toBe('perfil-pag2-1')
    expect(resultado[124].id).toBe('perfil-pag2-25')

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/perfiles?nombre=juan&page=0&size=100'),
      expect.any(Object),
    )
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/perfiles?nombre=juan&page=1&size=100'),
      expect.any(Object),
    )
  })

  it('lista contextos de estudiantes de forma masiva con y sin periodoId', async () => {
    sessionStorage.setItem('accessToken', 'token')
    const contextosMock = [
      {
        perfilId: 'p-1',
        estudianteId: 'e-1',
        carreraId: 'c-1',
        periodoId: 'per-1',
        nivel: 3,
        activo: true,
      },
    ]

    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(contextosMock),
    })
    vi.stubGlobal('fetch', fetchMock)

    const conPeriodo = await listarContextosEstudiantesMasivos('per-1')
    expect(conPeriodo).toEqual(contextosMock)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/estudiantes/contextos?periodoId=per-1'),
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer token' }),
      }),
    )

    await listarContextosEstudiantesMasivos()
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/estudiantes/contextos'),
      expect.any(Object),
    )
  })

  it('expone error HTTP del backend y fallo de red', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 409,
        json: vi.fn().mockResolvedValue({ message: 'Duplicado' }),
      }),
    )
    await expect(listarPerfiles()).rejects.toEqual(
      expect.objectContaining({ status: 409, message: 'Duplicado' }),
    )
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')))
    await expect(listarPerfiles()).rejects.toBeInstanceOf(UsuariosApiError)
  })
})
