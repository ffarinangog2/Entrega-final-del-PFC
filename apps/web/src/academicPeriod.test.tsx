import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AcademicPeriodProvider } from './academicPeriod'
import { useAcademicPeriod } from './academicPeriodContext'
import { AcademicPeriodSelector } from './components/AcademicPeriodSelector'
import { AuthContext } from './auth'
import type { AuthContextValue } from './auth/context'
import * as academico from './services/academicoApi'
import { ApiError } from './services/apiClient'

vi.mock('./services/academicoApi')

const periodoPPA: academico.PeriodoLectivo = {
  id: 'periodo-ppa',
  codigo: 'PPA-2026-2027-C1',
  nombre: 'Ciclo academico Mayo-Septiembre',
  fechaInicio: '2026-05-01',
  fechaFin: '2026-09-18',
  estado: 'ACTIVO',
  ppaCodigo: 'REGULAR-2026-2027-PPA',
  ppaNombre: 'REGULAR - 2026-2027 PPA',
  cicloAcademico: 1,
}

const periodoSPA: academico.PeriodoLectivo = {
  id: 'periodo-spa',
  codigo: 'PPA-2026-2027-C2',
  nombre: 'Ciclo academico Noviembre-Abril',
  fechaInicio: '2026-11-01',
  fechaFin: '2027-04-30',
  estado: 'PLANIFICADO',
  ppaCodigo: 'REGULAR-2026-2027-PPA',
  ppaNombre: 'REGULAR - 2026-2027 PPA',
  cicloAcademico: 2,
}

const periodoPasado: academico.PeriodoLectivo = {
  id: 'periodo-pasado',
  codigo: 'PPA-2025-2026-C2',
  nombre: 'Ciclo academico Noviembre-Abril',
  fechaInicio: '2025-11-01',
  fechaFin: '2026-04-30',
  estado: 'FINALIZADO',
  ppaCodigo: 'REGULAR-2025-2026-PPA',
  ppaNombre: 'REGULAR - 2025-2026 PPA',
  cicloAcademico: 2,
}

const periodoAjenoA: academico.PeriodoLectivo = {
  id: 'periodo-ajeno-a',
  codigo: '2026-A',
  nombre: 'Periodo Lectivo 2026-A',
  fechaInicio: '2026-01-01',
  fechaFin: '2026-12-31',
  estado: 'ACTIVO',
  cicloAcademico: null,
}

const periodoAjenoB: academico.PeriodoLectivo = {
  id: 'periodo-ajeno-b',
  codigo: '2026-B',
  nombre: 'Periodo Lectivo 2026-B',
  fechaInicio: '2026-01-01',
  fechaFin: '2026-12-31',
  estado: 'ACTIVO',
  cicloAcademico: null,
}

const authBase: AuthContextValue = {
  usuario: null,
  isAuthenticated: false,
  isLoading: true,
  login: vi.fn(),
  logout: vi.fn(),
  refreshSession: vi.fn(),
}

const authAutenticado: AuthContextValue = {
  ...authBase,
  usuario: { id: 'usr-1', nombre: 'Admin', roles: ['ROLE_COORDINADOR'] } as unknown as AuthContextValue['usuario'],
  isAuthenticated: true,
  isLoading: false,
}

function EstadoPeriodo() {
  const {
    periodoVigente,
    periodoSeleccionado,
    seleccionarPeriodo,
    cargando,
    error,
  } = useAcademicPeriod()
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
        onClick={() => seleccionarPeriodo('periodo-spa')}
      >
        Cambiar a SPA
      </button>
      <button
        type="button"
        onClick={() => seleccionarPeriodo('periodo-fantasma')}
      >
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
        <AcademicPeriodSelector />
      </AcademicPeriodProvider>
    </AuthContext.Provider>
  )
}

describe('AcademicPeriodProvider & AcademicPeriodSelector (ETAPA 5B)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    // Reloj determinista: 15 de julio de 2026 (dentro del rango de PPA: 2026-05-01 a 2026-09-18)
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.setSystemTime(new Date('2026-07-15T12:00:00Z'))
    vi.mocked(academico.obtenerPeriodos).mockResolvedValue([
      periodoPPA,
      periodoSPA,
      periodoPasado,
      periodoAjenoA,
      periodoAjenoB,
    ])
    vi.mocked(academico.obtenerPeriodoActual).mockResolvedValue(periodoPPA)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('espera la restauración de sesión antes de consultar el período', async () => {
    const rendered = render(vista(authBase))

    expect(screen.getByTestId('estado')).toHaveTextContent('cargando')
    expect(academico.obtenerPeriodoActual).not.toHaveBeenCalled()

    rendered.rerender(vista(authAutenticado))

    await waitFor(() => expect(academico.obtenerPeriodoActual).toHaveBeenCalledOnce())
    await waitFor(() =>
      expect(screen.getByTestId('estado')).toHaveTextContent('Ciclo academico Mayo-Septiembre'),
    )
  })

  it('distingue ausencia real de período de un error HTTP', async () => {
    vi.mocked(academico.obtenerPeriodoActual).mockRejectedValue(
      new ApiError(404, 'No hay período vigente'),
    )
    vi.mocked(academico.obtenerPeriodos).mockResolvedValue([])
    render(vista(authAutenticado))

    expect(await screen.findByTestId('estado')).toHaveTextContent('sin-periodo')
  })

  it('1. PPA vigente por fecha aparece', async () => {
    render(vista(authAutenticado))

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Período académico:/ })).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByRole('button', { name: /Período académico:/ }))

    const dialog = screen.getByRole('dialog', { name: 'Período académico' })
    expect(within(dialog).getByText('REGULAR 2026-2027 PPA')).toBeInTheDocument()
  })

  it('2. SPA futuro NO aparece', async () => {
    render(vista(authAutenticado))

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Período académico:/ })).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByRole('button', { name: /Período académico:/ }))

    const dialog = screen.getByRole('dialog', { name: 'Período académico' })
    expect(within(dialog).queryByText(/SPA/)).not.toBeInTheDocument()
  })

  it('3. al llegar fechaInicio del SPA, SPA puede aparecer', async () => {
    // Avanzar reloj al 15 de noviembre de 2026 (SPA inicio: 2026-11-01)
    vi.setSystemTime(new Date('2026-11-15T12:00:00Z'))
    vi.mocked(academico.obtenerPeriodoActual).mockResolvedValue(periodoSPA)

    render(vista(authAutenticado))

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Período académico:/ })).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByRole('button', { name: /Período académico:/ }))

    const dialog = screen.getByRole('dialog', { name: 'Período académico' })
    expect(within(dialog).getByText('REGULAR 2026-2027 SPA')).toBeInTheDocument()
    // PPA ya terminó, por tanto no debe figurar como disponible
    expect(within(dialog).queryByText('REGULAR 2026-2027 PPA')).not.toBeInTheDocument()
  })

  it('4. período cuya fechaFin ya pasó no aparece como disponible', async () => {
    render(vista(authAutenticado))

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Período académico:/ })).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByRole('button', { name: /Período académico:/ }))

    const dialog = screen.getByRole('dialog', { name: 'Período académico' })
    expect(within(dialog).queryByText('REGULAR 2025-2026 SPA')).not.toBeInTheDocument()
  })

  it('5. períodos ajenos 2026-A / 2026-B no aparecen si no pertenecen al flujo REGULAR PPA/SPA', async () => {
    render(vista(authAutenticado))

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Período académico:/ })).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByRole('button', { name: /Período académico:/ }))

    const dialog = screen.getByRole('dialog', { name: 'Período académico' })
    expect(within(dialog).queryByText(/2026-A/)).not.toBeInTheDocument()
    expect(within(dialog).queryByText(/2026-B/)).not.toBeInTheDocument()
  })

  it('6. con una sola opción, el panel sigue abriendo', async () => {
    render(vista(authAutenticado))

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Período académico:/ })).toBeInTheDocument(),
    )

    const trigger = screen.getByRole('button', { name: /Período académico:/ })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    fireEvent.click(trigger)

    expect(screen.getByRole('dialog', { name: 'Período académico' })).toBeInTheDocument()
    expect(screen.getAllByRole('option')).toHaveLength(1)
  })

  it('7. la opción válida aparece seleccionada', async () => {
    render(vista(authAutenticado))

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Período académico:/ })).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByRole('button', { name: /Período académico:/ }))

    const opcion = screen.getByRole('option', { name: /REGULAR 2026-2027 PPA/ })
    expect(opcion).toHaveAttribute('aria-selected', 'true')
    expect(opcion).toHaveClass('is-selected')
    expect(screen.getByLabelText('Seleccionado')).toBeInTheDocument()
  })

  it('8. no existe POST/PUT/PATCH', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    render(vista(authAutenticado))

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Período académico:/ })).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByRole('button', { name: /Período académico:/ }))
    const opcion = screen.getByRole('option', { name: /REGULAR 2026-2027 PPA/ })
    fireEvent.click(opcion)

    expect(fetchSpy).not.toHaveBeenCalled()
    fetchSpy.mockRestore()
  })

  it('9. no se crean objetos ficticios', async () => {
    render(vista(authAutenticado))

    await waitFor(() =>
      expect(screen.getByTestId('seleccionado')).toHaveTextContent('Ciclo academico Mayo-Septiembre'),
    )

    fireEvent.click(screen.getByRole('button', { name: 'Cambiar a Fantasma' }))

    // El seleccionado permanece intacto y no muta a ningún objeto ficticio
    expect(screen.getByTestId('seleccionado')).toHaveTextContent('Ciclo academico Mayo-Septiembre')
  })

  it('10. con cero períodos válidos aparece el estado vacío', async () => {
    // Simular que no hay períodos válidos en catálogo ni vigente
    vi.mocked(academico.obtenerPeriodos).mockResolvedValue([periodoAjenoA, periodoAjenoB])
    vi.mocked(academico.obtenerPeriodoActual).mockResolvedValue(periodoAjenoA)

    render(vista(authAutenticado))

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Período académico:/ })).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByRole('button', { name: /Período académico:/ }))

    expect(screen.getByRole('dialog', { name: 'Período académico' })).toBeInTheDocument()
    expect(screen.getByText('No hay períodos académicos disponibles')).toBeInTheDocument()
  })
})
