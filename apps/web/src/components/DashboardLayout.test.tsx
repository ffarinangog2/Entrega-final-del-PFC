import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AuthUser } from '../types/auth'
import { DashboardLayout } from './DashboardLayout'

let usuario: AuthUser
vi.mock('../auth', async (original) => ({ ...(await original<typeof import('../auth')>()), useAuth: () => ({ usuario }) }))
vi.mock('./LogoutButton', () => ({ LogoutButton: () => <button>Salir</button> }))
const base = { id: 'u', perfilId: 'p', username: 'user', nombres: 'Demo', apellidos: '', emailInstitucional: '', tiposPerfil: [] }
function menu(roles: string[], permisos: string[]) { usuario = { ...base, roles, permisos }; render(<MemoryRouter><DashboardLayout breadcrumb="Test"><p>Contenido</p></DashboardLayout></MemoryRouter>) }

describe('DashboardLayout por permisos', () => {
  beforeEach(() => vi.clearAllMocks())
  it('DOCENTE ve Nueva solicitud', () => { menu(['DOCENTE'], ['ACADEMICO_LEER', 'SOLICITUD_CREAR', 'SOLICITUD_LEER', 'RESERVA_LEER']); expect(screen.getByRole('link', { name: /Nueva solicitud/ })).toBeInTheDocument() })
  it('ESTUDIANTE no ve Nueva solicitud', () => { menu(['ESTUDIANTE'], ['ACADEMICO_LEER']); expect(screen.queryByRole('link', { name: /Nueva solicitud/ })).not.toBeInTheDocument() })
  it('ADMINISTRADOR_PISO ve gestión y calendario', () => { menu(['ADMINISTRADOR_PISO'], ['SOLICITUD_LEER', 'SOLICITUD_APROBAR', 'RESERVA_LEER', 'AGENDA_GESTIONAR']); expect(screen.getByRole('link', { name: /Reservas/ })).toBeInTheDocument(); expect(screen.getByRole('link', { name: /Calendario/ })).toBeInTheDocument() })
  it.each(['DECANO', 'COORDINADOR'])('%s no ve operaciones diarias sin permiso', (rol) => { menu([rol], ['ACADEMICO_LEER']); expect(screen.queryByRole('link', { name: /Reservas/ })).not.toBeInTheDocument(); expect(screen.queryByRole('link', { name: /Nueva solicitud/ })).not.toBeInTheDocument() })
})
