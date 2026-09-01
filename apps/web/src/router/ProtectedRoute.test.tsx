import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AuthContext } from '../auth/context'
import { ProtectedRoute } from './ProtectedRoute'

function renderRoute(permisos: string[]) {
  const usuario = { id: 'u', perfilId: 'p', username: 'demo', nombres: '', apellidos: '', emailInstitucional: '', roles: ['DOCENTE'], permisos, tiposPerfil: [] }
  render(<AuthContext.Provider value={{ usuario, isAuthenticated: true, isLoading: false, login: async () => {}, logout: async () => {}, refreshSession: async () => true }}><MemoryRouter initialEntries={['/reservas/nueva']}><Routes><Route path="/main" element={<p>Inicio seguro</p>} /><Route element={<ProtectedRoute permissions={['SOLICITUD_CREAR']} />}><Route path="/reservas/nueva" element={<p>Formulario protegido</p>} /></Route></Routes></MemoryRouter></AuthContext.Provider>)
}
describe('ProtectedRoute por permisos', () => {
  it('permite URL directa con permiso', () => { renderRoute(['SOLICITUD_CREAR']); expect(screen.getByText('Formulario protegido')).toBeInTheDocument() })
  it('muestra una vista 403 sin permiso', () => { renderRoute([]); expect(screen.getByRole('heading', { name: 'No tienes permiso' })).toBeInTheDocument() })
})
