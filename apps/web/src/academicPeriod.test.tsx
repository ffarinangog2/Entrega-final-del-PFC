import { fireEvent, render, screen, waitFor } from '@testing-library/react'
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

const periodo2: academico.PeriodoLectivo = {
  id: 'periodo-secundario',
  codigo: 'REGULAR-2026-2027-SPA',
  nombre: 'REGULAR 2026-2027 SPA',
  fechaInicio: '2026-11-01',
  fechaFin: '2027-04-30',
  estado: 'PLANIFICADO',
  ppaCodigo: 'SPA',
  ppaNombre: 'SPA',
  cicloAcademico: 2,
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
  const { periodoVigente, periodoSeleccionado, seleccionarPeriodo, cargando, error } =
    useAcademicPeriod()
  return (
    <div>
      <p data-testid="estado">
        {cargando
          ? 'cargando'
          : error ?? periodoVigente?.nombre ?? 'sin-periodo'}
      </p>
      <p data-testid="seleccionado">{periodoSeleccionado?.nombre ?? 'ninguno'}</p>
      <button
        type="button"
        onClick={() => seleccionarPeriodo('periodo-secundario')}
      >
        Cambiar a Secundario
      </button>
      <button type="button" onClick={() => seleccionarPeriodo('periodo-fantasma')}>
        Cambiar a Fantasma
      </button>
    </div>
  )
}

function vista(auth: AuthContextValue) {
  return (
    <AuthContext.Provider value={auth}>
      <AcademicPeriodProvider>
        <EstadoPeriodo />
      </AcademicPeriodProvider>
    </AuthContext.Provider>
  )
}

describe('AcademicPeriodProvider', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(academico.obtenerPeriodos).mockResolvedValue([periodo, periodo2])
  })

  it('espera la restauración de sesión antes de consultar el período', async () => {
    vi.mocked(academico.obtenerPeriodoActual).mockResolvedValue(periodo)
    const rendered = render(vista(authBase))

    expect(screen.getByTestId('estado')).toHaveTextContent('cargando')
    expect(academico.obtenerPeriodoActual).not.toHaveBeenCalled()

    rendered.rerender(
      vista({
        ...authBase,
        usuario: {} as AuthContextValue['usuario'],
        isAuthenticated: true,
        isLoading: false,
      }),
    )

    await waitFor(() => expect(academico.obtenerPeriodoActual).toHaveBeenCalledOnce())
    await waitFor(() =>
      expect(screen.getByTestId('estado')).toHaveTextContent('REGULAR 2026-2027 PPA'),
    )
  })

  it('consulta normalmente cuando la sesión ya está autenticada', async () => {
    vi.mocked(academico.obtenerPeriodoActual).mockResolvedValue(periodo)
    render(
      vista({
        ...authBase,
        usuario: {} as AuthContextValue['usuario'],
        isAuthenticated: true,
        isLoading: false,
      }),
    )

    await waitFor(() =>
      expect(screen.getByTestId('estado')).toHaveTextContent('REGULAR 2026-2027 PPA'),
    )
    expect(academico.obtenerPeriodoActual).toHaveBeenCalledOnce()
  })

  it('distingue ausencia real de período de un error HTTP', async () => {
    vi.mocked(academico.obtenerPeriodoActual).mockRejectedValue(
      new ApiError(404, 'No hay período vigente'),
    )
    vi.mocked(academico.obtenerPeriodos).mockResolvedValue([])
    render(
      vista({
        ...authBase,
        usuario: {} as AuthContextValue['usuario'],
        isAuthenticated: true,
        isLoading: false,
      }),
    )

    expect(await screen.findByTestId('estado')).toHaveTextContent('sin-periodo')
  })

  it('seleccionarPeriodo actualiza periodoSeleccionado en contexto sin llamar al backend', async () => {
    vi.mocked(academico.obtenerPeriodoActual).mockResolvedValue(periodo)
    render(
      vista({
        ...authBase,
        usuario: {} as AuthContextValue['usuario'],
        isAuthenticated: true,
        isLoading: false,
      }),
    )

    await waitFor(() =>
      expect(screen.getByTestId('seleccionado')).toHaveTextContent('REGULAR 2026-2027 PPA'),
    )

    fireEvent.click(screen.getByRole('button', { name: 'Cambiar a Secundario' }))

    expect(screen.getByTestId('seleccionado')).toHaveTextContent('REGULAR 2026-2027 SPA')
    // No debe haber más llamadas al backend
    expect(academico.obtenerPeriodoActual).toHaveBeenCalledOnce()
  })

  it('seleccionarPeriodo ignora ids inexistentes sin alterar el seleccionado ni crear ficticios', async () => {
    vi.mocked(academico.obtenerPeriodoActual).mockResolvedValue(periodo)
    render(
      vista({
        ...authBase,
        usuario: {} as AuthContextValue['usuario'],
        isAuthenticated: true,
        isLoading: false,
      }),
    )

    await waitFor(() =>
      expect(screen.getByTestId('seleccionado')).toHaveTextContent('REGULAR 2026-2027 PPA'),
    )

    fireEvent.click(screen.getByRole('button', { name: 'Cambiar a Fantasma' }))

    // Sigue siendo el original
    expect(screen.getByTestId('seleccionado')).toHaveTextContent('REGULAR 2026-2027 PPA')
  })
})
