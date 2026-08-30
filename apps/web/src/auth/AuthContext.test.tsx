import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from './AuthContext'
import { useAuth } from './useAuth'
import { login, logout, refresh } from '../services/authApi'

vi.mock('../services/authApi', () => ({ login: vi.fn(), logout: vi.fn(), refresh: vi.fn() }))

const user = {
  id: 'u1', perfilId: 'p1', username: 'admin', nombres: 'Admin',
  apellidos: 'SCLI', emailInstitucional: 'admin@test', roles: ['ADMIN'],
  permisos: ['RESERVA_LEER'], tiposPerfil: ['ADMINISTRADOR'],
}
const response = {
  tokenType: 'Bearer', accessToken: 'access', refreshToken: 'refresh',
  expiresIn: 3600, usuario: user,
}

function Probe() {
  const auth = useAuth()
  return (
    <>
      <span data-testid="state">{auth.isLoading ? 'loading' : auth.usuario?.username ?? 'anonymous'}</span>
      <button onClick={() => void auth.login('admin', 'pass')}>login</button>
      <button onClick={auth.logout}>logout</button>
      <button onClick={() => void auth.refreshSession()}>refresh</button>
    </>
  )
}

function renderProvider() {
  return render(<AuthProvider><Probe /></AuthProvider>)
}

describe('AuthProvider', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.clearAllMocks()
  })

  it('revoca remotamente al cerrar sesión y después limpia la sesión local', async () => {
    vi.mocked(login).mockResolvedValue(response)
    vi.mocked(logout).mockResolvedValue(undefined)
    renderProvider()
    await screen.findByText('anonymous')
    fireEvent.click(screen.getByText('login'))
    await screen.findByText('admin')
    expect(sessionStorage.getItem('accessToken')).toBe('access')
    expect(sessionStorage.getItem('refreshToken')).toBe('refresh')
    expect(sessionStorage.getItem('usuario')).toContain('"username":"admin"')
    fireEvent.click(screen.getByText('logout'))
    await screen.findByText('anonymous')
    expect(logout).toHaveBeenCalledWith('refresh')
    expect(sessionStorage.length).toBe(0)
  })

  it('limpia la sesión local aunque falle la revocación remota', async () => {
    sessionStorage.setItem('usuario', JSON.stringify(user))
    sessionStorage.setItem('expiresAt', String(Date.now() + 60_000))
    sessionStorage.setItem('accessToken', 'access')
    sessionStorage.setItem('refreshToken', 'refresh')
    vi.mocked(logout).mockRejectedValue(new Error('offline'))
    renderProvider()
    await screen.findByText('admin')

    fireEvent.click(screen.getByText('logout'))

    await screen.findByText('anonymous')
    expect(logout).toHaveBeenCalledWith('refresh')
    expect(sessionStorage.length).toBe(0)
  })

  it('restaura una sesión vigente sin solicitar refresh', async () => {
    sessionStorage.setItem('usuario', JSON.stringify(user))
    sessionStorage.setItem('expiresAt', String(Date.now() + 60_000))
    renderProvider()
    await screen.findByText('admin')
    expect(refresh).not.toHaveBeenCalled()
  })

  it('renueva una sesión expirada y almacena los tokens nuevos', async () => {
    sessionStorage.setItem('usuario', JSON.stringify(user))
    sessionStorage.setItem('expiresAt', String(Date.now() - 1))
    sessionStorage.setItem('refreshToken', 'old-refresh')
    vi.mocked(refresh).mockResolvedValue(response)
    renderProvider()
    await waitFor(() => expect(refresh).toHaveBeenCalledWith('old-refresh'))
    await screen.findByText('admin')
    expect(sessionStorage.getItem('accessToken')).toBe('access')
  })

  it('cierra la sesión si refresh falla o no existe token', async () => {
    sessionStorage.setItem('usuario', JSON.stringify(user))
    sessionStorage.setItem('expiresAt', String(Date.now() + 60_000))
    sessionStorage.setItem('refreshToken', 'bad-refresh')
    vi.mocked(refresh).mockRejectedValue(new Error('expired'))
    renderProvider()
    await screen.findByText('admin')
    fireEvent.click(screen.getByText('refresh'))
    await screen.findByText('anonymous')
    expect(sessionStorage.length).toBe(0)

    fireEvent.click(screen.getByText('refresh'))
    await act(async () => undefined)
    expect(refresh).toHaveBeenCalledTimes(1)
  })

  it('descarta una sesión almacenada con JSON inválido', async () => {
    sessionStorage.setItem('usuario', '{invalid')
    sessionStorage.setItem('accessToken', 'stale')
    renderProvider()
    await screen.findByText('anonymous')
    expect(sessionStorage.length).toBe(0)
  })
})
