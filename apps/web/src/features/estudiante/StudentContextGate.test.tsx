import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { StudentContextGate } from './StudentContextGate'
import * as academicPeriodModule from '../../academicPeriodContext'
import * as academicoApiModule from '../../services/academicoApi'
import * as usuariosApiModule from '../../services/usuariosApi'

vi.mock('../../academicPeriodContext', () => ({
  useAcademicPeriod: vi.fn(),
}))

vi.mock('../../services/academicoApi', () => ({
  obtenerCarreras: vi.fn(),
}))

vi.mock('../../services/usuariosApi', () => ({
  obtenerMisContextosAcademicos: vi.fn(),
  confirmarMiContextoAcademico: vi.fn(),
}))

const mockPeriodoVigente: academicoApiModule.PeriodoLectivo = {
  id: 'periodo-vigente-1',
  codigo: '2026-1',
  nombre: '2026 CI',
  fechaInicio: '2026-01-01',
  fechaFin: '2026-06-30',
  estado: 'ACTIVO',
  cicloAcademico: 1,
  ppaNombre: '2026-1 PPA',
}

const mockCarreras: academicoApiModule.Carrera[] = [
  { id: 'carrera-1', facultadId: 'fac-1', codigo: 'SOF', nombre: 'Ingeniería de Software', activo: true },
  { id: 'carrera-2', facultadId: 'fac-1', codigo: 'TEL', nombre: 'Telecomunicaciones', activo: false },
]

describe('StudentContextGate', () => {
  it('renderiza children directamente si periodoVigente es null', () => {
    vi.mocked(academicPeriodModule.useAcademicPeriod).mockReturnValue({
      periodos: [],
      periodoVigente: null,
      periodoSeleccionado: null,
      seleccionarPeriodo: vi.fn(),
      cargando: false,
    })

    render(
      <StudentContextGate>
        <div>Contenido Protegido</div>
      </StudentContextGate>
    )

    expect(screen.getByText('Contenido Protegido')).toBeInTheDocument()
  })

  it('renderiza children si el estudiante ya tiene contexto para el periodo vigente', async () => {
    vi.mocked(academicPeriodModule.useAcademicPeriod).mockReturnValue({
      periodos: [mockPeriodoVigente],
      periodoVigente: mockPeriodoVigente,
      periodoSeleccionado: mockPeriodoVigente,
      seleccionarPeriodo: vi.fn(),
      cargando: false,
    })
    vi.mocked(usuariosApiModule.obtenerMisContextosAcademicos).mockResolvedValue([
      {
        id: 'ctx-1',
        estudianteId: 'est-1',
        carreraId: 'carrera-1',
        periodoId: 'periodo-vigente-1',
        nivel: 3,
        activo: true,
        creadoEn: '2026-01-01',
      },
    ])
    vi.mocked(academicoApiModule.obtenerCarreras).mockResolvedValue(mockCarreras)

    render(
      <StudentContextGate>
        <div>Contenido Protegido</div>
      </StudentContextGate>
    )

    await waitFor(() => {
      expect(screen.getByText('Contenido Protegido')).toBeInTheDocument()
    })
  })

  it('muestra formulario de confirmacion cuando no tiene contexto y permite autodeclararlo', async () => {
    vi.mocked(academicPeriodModule.useAcademicPeriod).mockReturnValue({
      periodos: [mockPeriodoVigente],
      periodoVigente: mockPeriodoVigente,
      periodoSeleccionado: mockPeriodoVigente,
      seleccionarPeriodo: vi.fn(),
      cargando: false,
    })
    vi.mocked(usuariosApiModule.obtenerMisContextosAcademicos).mockResolvedValue([])
    vi.mocked(academicoApiModule.obtenerCarreras).mockResolvedValue(mockCarreras)
    vi.mocked(usuariosApiModule.confirmarMiContextoAcademico).mockResolvedValue({
      id: 'ctx-new',
      estudianteId: 'est-1',
      carreraId: 'carrera-1',
      periodoId: 'periodo-vigente-1',
      nivel: 2,
      activo: true,
      creadoEn: '2026-01-01',
    })

    render(
      <StudentContextGate>
        <div>Contenido Protegido</div>
      </StudentContextGate>
    )

    await waitFor(() => {
      expect(screen.getByText('Confirma tu contexto académico')).toBeInTheDocument()
    })

    expect(screen.queryByText('Contenido Protegido')).not.toBeInTheDocument()

    const selectNivel = screen.getByLabelText(/Nivel actual/i)
    fireEvent.change(selectNivel, { target: { value: '2' } })

    const botonGuardar = screen.getByText('Guardar y continuar')
    fireEvent.click(botonGuardar)

    await waitFor(() => {
      expect(usuariosApiModule.confirmarMiContextoAcademico).toHaveBeenCalledWith({
        carreraId: 'carrera-1',
        periodoId: 'periodo-vigente-1',
        nivel: 2,
      })
    })

    await waitFor(() => {
      expect(screen.getByText('Contenido Protegido')).toBeInTheDocument()
    })
  })

  it('muestra mensaje de error si el guardado falla', async () => {
    vi.mocked(academicPeriodModule.useAcademicPeriod).mockReturnValue({
      periodos: [mockPeriodoVigente],
      periodoVigente: mockPeriodoVigente,
      periodoSeleccionado: mockPeriodoVigente,
      seleccionarPeriodo: vi.fn(),
      cargando: false,
    })
    vi.mocked(usuariosApiModule.obtenerMisContextosAcademicos).mockResolvedValue([])
    vi.mocked(academicoApiModule.obtenerCarreras).mockResolvedValue(mockCarreras)
    vi.mocked(usuariosApiModule.confirmarMiContextoAcademico).mockRejectedValue(new Error('Carrera inactiva'))

    render(
      <StudentContextGate>
        <div>Contenido Protegido</div>
      </StudentContextGate>
    )

    await waitFor(() => {
      expect(screen.getByText('Confirma tu contexto académico')).toBeInTheDocument()
    })

    const botonGuardar = screen.getByText('Guardar y continuar')
    fireEvent.click(botonGuardar)

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Carrera inactiva')
    })
  })

  it('muestra mensaje genérico si el guardado falla con excepción no estándar', async () => {
    vi.mocked(academicPeriodModule.useAcademicPeriod).mockReturnValue({
      periodos: [mockPeriodoVigente],
      periodoVigente: mockPeriodoVigente,
      periodoSeleccionado: mockPeriodoVigente,
      seleccionarPeriodo: vi.fn(),
      cargando: false,
    })
    vi.mocked(usuariosApiModule.obtenerMisContextosAcademicos).mockResolvedValue([])
    vi.mocked(academicoApiModule.obtenerCarreras).mockResolvedValue(mockCarreras)
    vi.mocked(usuariosApiModule.confirmarMiContextoAcademico).mockRejectedValue('error-desconocido')

    render(
      <StudentContextGate>
        <div>Contenido Protegido</div>
      </StudentContextGate>
    )

    await waitFor(() => {
      expect(screen.getByText('Confirma tu contexto académico')).toBeInTheDocument()
    })

    const botonGuardar = screen.getByText('Guardar y continuar')
    fireEvent.click(botonGuardar)

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('No se pudo guardar el contexto académico.')
    })
  })
})
