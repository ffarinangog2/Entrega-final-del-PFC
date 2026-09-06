import { render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AcademicPeriodProvider } from './academicPeriod'
import { useAcademicPeriod } from './academicPeriodContext'
import { AuthContext } from './auth'
import type { AuthContextValue } from './auth/context'
import * as academico from './services/academicoApi'
import { ApiError } from './services/apiClient'

vi.mock('./services/academicoApi')

const periodo: academico.PeriodoLectivo = {
  id: 'periodo-actual',
  codigo: 'REGULAR-2026-2027-PPA',
  nombre: 'REGULAR 2026-2027 PPA',
  fechaInicio: '2026-05-01',
  fechaFin: '2026-10-31',
  estado: 'ACTIVO',
  ppaCodigo: 'PPA',
  ppaNombre: 'PPA',
  cicloAcademico: 1,
}

const authBase: AuthContextValue = {
  usuario: null,
  isAuthenticated: false,
  isLoading: true,
  login: vi.fn(),
  logout: vi.fn(),
  refreshSession: vi.fn(),
}

function EstadoPeriodo() {
  const { periodoVigente, cargando, error } = useAcademicPeriod()
  return <p>{cargando ? 'cargando' : error ?? periodoVigente?.nombre ?? 'sin-periodo'}</p>
}

function vista(auth: AuthContextValue) {
  return (
    <AuthContext.Provider value={auth}>
      <AcademicPeriodProvider><EstadoPeriodo /></AcademicPeriodProvider>
    </AuthContext.Provider>
  )
}

describe('AcademicPeriodProvider', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('espera la restauración de sesión antes de consultar el período', async () => {
    vi.mocked(academico.obtenerPeriodoActual).mockResolvedValue(periodo)
    const rendered = render(vista(authBase))

    expect(screen.getByText('cargando')).toBeInTheDocument()
    expect(academico.obtenerPeriodoActual).not.toHaveBeenCalled()

    rendered.rerender(vista({ ...authBase, usuario: {} as AuthContextValue['usuario'], isAuthenticated: true, isLoading: false }))

    await waitFor(() => expect(academico.obtenerPeriodoActual).toHaveBeenCalledOnce())
    expect(await screen.findByText('REGULAR 2026-2027 PPA')).toBeInTheDocument()
  })

  it('consulta normalmente cuando la sesión ya está autenticada', async () => {
    vi.mocked(academico.obtenerPeriodoActual).mockResolvedValue(periodo)
    render(vista({ ...authBase, usuario: {} as AuthContextValue['usuario'], isAuthenticated: true, isLoading: false }))

    expect(await screen.findByText('REGULAR 2026-2027 PPA')).toBeInTheDocument()
    expect(academico.obtenerPeriodoActual).toHaveBeenCalledOnce()
  })

  it('distingue ausencia real de período de un error HTTP', async () => {
    vi.mocked(academico.obtenerPeriodoActual).mockRejectedValue(new ApiError(404, 'No hay período vigente'))
    render(vista({ ...authBase, usuario: {} as AuthContextValue['usuario'], isAuthenticated: true, isLoading: false }))

    expect(await screen.findByText('sin-periodo')).toBeInTheDocument()
  })
})
