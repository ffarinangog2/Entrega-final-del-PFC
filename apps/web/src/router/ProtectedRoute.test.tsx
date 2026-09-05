import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AuthContext } from '../auth/context'
import { ProtectedRoute } from './ProtectedRoute'

function renderRoute(permisos: string[], options: { authenticated?: boolean; loading?: boolean; roles?: string[]; requiredRoles?: string[] } = {}) {
  const usuario = { id: 'u', perfilId: 'p', username: 'demo', nombres: '', apellidos: '', emailInstitucional: '', roles: options.roles ?? ['DOCENTE'], permisos, tiposPerfil: [] }
  return render(<AuthContext.Provider value={{ usuario, isAuthenticated: options.authenticated ?? true, isLoading: options.loading ?? false, login: async () => {}, logout: async () => {}, refreshSession: async () => true }}><MemoryRouter initialEntries={['/reservas/nueva']}><Routes><Route path="/login" element={<p>Inicio de sesión</p>} /><Route path="/main" element={<p>Inicio seguro</p>} /><Route element={<ProtectedRoute permissions={['SOLICITUD_CREAR']} roles={options.requiredRoles} />}><Route path="/reservas/nueva" element={<p>Formulario protegido</p>} /></Route></Routes></MemoryRouter></AuthContext.Provider>)
}
describe('ProtectedRoute por permisos', () => {
  it('permite URL directa con permiso', () => { renderRoute(['SOLICITUD_CREAR']); expect(screen.getByText('Formulario protegido')).toBeInTheDocument() })
  it('muestra una vista 403 sin permiso', () => { renderRoute([]); expect(screen.getByRole('heading', { name: 'No tienes permiso' })).toBeInTheDocument() })
  it('muestra carga y redirige al login cuando corresponde', () => {
    const { unmount } = renderRoute([], { loading: true })
    expect(screen.getByText('Cargando sesión...')).toBeInTheDocument()
    unmount()
    renderRoute([], { authenticated: false })
    expect(screen.getByText('Inicio de sesión')).toBeInTheDocument()
  })
  it('valida también los roles requeridos', () => {
    renderRoute(['SOLICITUD_CREAR'], { roles: ['ESTUDIANTE'], requiredRoles: ['DOCENTE'] })
    expect(screen.getByRole('heading', { name: 'No tienes permiso' })).toBeInTheDocument()
  })
})
