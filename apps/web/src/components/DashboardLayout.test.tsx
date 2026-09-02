import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AuthUser } from '../types/auth'
import { DashboardLayout } from './DashboardLayout'

let usuario: AuthUser
vi.mock('../auth', async (original) => ({
  ...(await original<typeof import('../auth')>()),
  useAuth: () => ({ usuario }),
}))
vi.mock('./LogoutButton', () => ({
  LogoutButton: () => <button>Salir</button>,
}))
const base = {
  id: 'u',
  perfilId: 'p',
  username: 'user',
  nombres: 'Demo',
  apellidos: '',
  emailInstitucional: '',
  tiposPerfil: [],
}
function menu(roles: string[], permisos: string[]) {
  usuario = { ...base, roles, permisos }
  render(
    <MemoryRouter>
      <DashboardLayout breadcrumb="Test">
        <p>Contenido</p>
      </DashboardLayout>
    </MemoryRouter>,
  )
}

describe('DashboardLayout por permisos', () => {
  beforeEach(() => vi.clearAllMocks())
  it('ADMINISTRADOR ve navegación global', () => {
    menu(
      ['ADMINISTRADOR'],
      [
        'USUARIO_LEER',
        'ACADEMICO_LEER',
        'SOLICITUD_LEER',
        'RESERVA_LEER',
        'AGENDA_GESTIONAR',
        'PLANIFICACION_GESTIONAR',
        'INCIDENTE_LEER',
      ],
    )
    expect(screen.getByRole('link', { name: /Usuarios/ })).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: /Planificación/ }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: /Configuración/ }),
    ).toBeInTheDocument()
  })
  it('ADMINISTRADOR_PISO no ve asistencia ni configuración', () => {
    menu(
      ['ADMINISTRADOR_PISO'],
      [
        'ACADEMICO_LEER',
        'SOLICITUD_LEER',
        'SOLICITUD_APROBAR',
        'RESERVA_LEER',
        'AGENDA_GESTIONAR',
        'INCIDENTE_LEER',
      ],
    )
    expect(screen.getByRole('link', { name: /Reservas/ })).toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: /Asistencia/ }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: /Configuración/ }),
    ).not.toBeInTheDocument()
  })
  it('COORDINADOR ve planificación y no operaciones diarias', () => {
    menu(['COORDINADOR'], ['ACADEMICO_LEER', 'PLANIFICACION_GESTIONAR'])
    expect(
      screen.getByRole('link', { name: /Planificación/ }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: /Reservas/ }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: /Incidentes/ }),
    ).not.toBeInTheDocument()
  })
  it('DOCENTE ve reservas, nueva solicitud y asistencia', () => {
    menu(
      ['DOCENTE'],
      [
        'ACADEMICO_LEER',
        'SOLICITUD_CREAR',
        'SOLICITUD_LEER',
        'RESERVA_LEER',
        'INCIDENTE_CREAR',
      ],
    )
    expect(
      screen.getByRole('link', { name: /Nueva solicitud/ }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: /^Asistencia$/ }),
    ).toBeInTheDocument()
  })
  it('ESTUDIANTE ve registro y perfil, no operaciones sin datos académicos seguros', () => {
    menu(['ESTUDIANTE'], ['ACADEMICO_LEER'])
    expect(
      screen.getByRole('link', { name: /Registro e historial/ }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: /Mi horario/ }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: /Reservas/ }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: /Planificación/ }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: /Incidentes/ }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: /Laboratorios/ }),
    ).not.toBeInTheDocument()
  })
})
