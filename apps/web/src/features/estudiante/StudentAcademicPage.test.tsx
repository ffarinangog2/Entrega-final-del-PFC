import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as academico from '../../services/academicoApi'
import * as operational from '../../services/operationalApi'
import * as usuarios from '../../services/usuariosApi'
import {
  MiHorarioPage,
  StudentHistorialPage,
  StudentLaboratoriosPage,
} from './StudentAcademicPage'
import { AcademicPeriodContext } from '../../academicPeriodContext'

vi.mock('../../services/usuariosApi')
vi.mock('../../services/academicoApi')
vi.mock('../../services/operationalApi')
vi.mock('../../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => (
    <>{children}</>
  ),
}))

const mockContexto = {
  id: 'ctx-1',
  estudianteId: 'est-1',
  carreraId: 'carr-1',
  periodoId: 'c1',
  nivel: 4,
  activo: true,
  creadoEn: '2026-03-01T00:00:00Z',
}

const mockPeriodoActual = {
  id: 'c1',
  codigo: 'PPA',
  nombre: 'REGULAR 2026-2027 PPA',
  fechaInicio: '2026-04-01',
  fechaFin: '2026-08-31',
  estado: 'ACTIVO' as const,
}

const mockBloques: operational.Planificacion[] = [
  {
    id: 'b-1',
    planificacionId: 'plan-1',
    carreraId: 'carr-1',
    periodoId: 'c1',
    nivel: 4,
    materiaId: 'mat-1',
    docenteId: 'doc-1',
    laboratorioId: 'lab-1',
    diaSemana: 'LUNES',
    horaInicio: '07:30',
    horaFin: '09:30',
    estado: 'CONFIRMADA',
    observacion: null,
    version: 1,
  },
]

describe('StudentAcademicPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(usuarios.obtenerMiContextoAcademico).mockResolvedValue(
      mockContexto,
    )
    vi.mocked(usuarios.obtenerMisContextosAcademicos).mockResolvedValue([
      mockContexto,
      {
        id: 'ctx-0',
        estudianteId: 'est-1',
        carreraId: 'carr-1',
        periodoId: 'c0',
        nivel: 3,
        activo: false,
        creadoEn: '2025-09-01T00:00:00Z',
      },
    ])
    vi.mocked(academico.obtenerPeriodoActual).mockResolvedValue(
      mockPeriodoActual,
    )
    vi.mocked(academico.obtenerPeriodos).mockResolvedValue([
      mockPeriodoActual,
      {
        id: 'c0',
        codigo: '2025-B',
        nombre: 'Periodo Anterior',
        fechaInicio: '',
        fechaFin: '',
        estado: 'FINALIZADO',
      },
    ])
    vi.mocked(academico.obtenerCarreras).mockResolvedValue([
      {
        id: 'carr-1',
        facultadId: 'fac-1',
        codigo: 'SOF',
        nombre: 'Ingeniería de Software',
        activo: true,
      },
    ])
    vi.mocked(academico.obtenerMaterias).mockResolvedValue([
      {
        id: 'mat-1',
        carreraId: 'carr-1',
        codigo: 'SOF401',
        nombre: 'Aplicaciones Web',
        numeroHoras: 4,
        nivel: 4,
        activo: true,
      },
      {
        id: 'mat-migrada',
        carreraId: 'carr-1',
        codigo: 'SOF402',
        nombre: 'Sistemas Operativos',
        numeroHoras: 4,
        nivel: 4,
        activo: true,
      },
    ])
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([
      {
        id: 'lab-1',
        pisoId: 'piso-0',
        codigo: 'L-PB01',
        nombre: 'Lab Fundamentos',
        capacidad: 25,
        descripcion: 'Planta Baja',
        estado: 'DISPONIBLE',
        activo: true,
        creadoEn: '',
        actualizadoEn: '',
      },
      {
        id: 'lab-p2-migrado',
        pisoId: 'piso-1',
        codigo: 'L-P201',
        nombre: 'Lab Redes Nuevas',
        capacidad: 30,
        descripcion: 'Piso 2',
        estado: 'DISPONIBLE',
        activo: true,
        creadoEn: '',
        actualizadoEn: '',
      },
    ])
    vi.mocked(academico.obtenerPisos).mockResolvedValue([
      {
        id: 'piso-0',
        bloqueId: 'b-1',
        numero: 0,
        descripcion: 'Planta Baja',
        activo: true,
      },
      {
        id: 'piso-1',
        bloqueId: 'b-1',
        numero: 1,
        descripcion: 'Piso 2',
        activo: true,
      },
    ])
    vi.mocked(usuarios.obtenerDocenteResumen).mockResolvedValue({
      id: 'doc-1',
      nombres: 'María',
      apellidos: 'López',
      codigoDocente: 'DOC-001',
    })
    vi.mocked(operational.obtenerMiHorario).mockResolvedValue(mockBloques)
    vi.mocked(operational.historialAsistencia).mockResolvedValue([
      {
        id: 'asist-1',
        sesionId: 'ses-1',
        estudianteId: 'est-1',
        bloqueId: 'b-1',
        registradaEn: '2026-05-10T07:35:00Z',
        estado: 'REGISTRADA',
      },
    ])
  })

  it('deriva horario aprobado con nombres humanos y detalle de solo lectura', async () => {
    render(<MiHorarioPage />)

    expect(await screen.findByText('Aplicaciones Web')).toBeInTheDocument()
    expect(screen.getByText(/L-PB01/)).toBeInTheDocument()
    expect(screen.getByText(/Piso 1 · Planta Baja/)).toBeInTheDocument()
    expect(await screen.findByText('María López')).toBeInTheDocument()

    // Verificación de solo lectura estricta: estudiante no ve botones de edición
    expect(
      screen.queryByRole('button', { name: /editar/i }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: /agregar/i }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: /eliminar/i }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: /guardar/i }),
    ).not.toBeInTheDocument()
    // No debe haber un selector de ciclo redundante en Mi Horario
    expect(screen.queryByLabelText('Ciclo consultado')).not.toBeInTheDocument()

    // Clic en la celda del horario para ver modal de detalle de solo lectura
    fireEvent.click(screen.getByText('Aplicaciones Web'))
    expect(
      await screen.findByRole('region', { name: 'Detalle de clase' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Solo lectura')).toBeInTheDocument()
    expect(
      screen.getAllByText(/Piso 1 · Planta Baja/)[0],
    ).toBeInTheDocument()
  })

  it('refleja bloque migrado mostrando el nuevo laboratorio y nuevo piso', async () => {
    const bloqueMigrado: operational.Planificacion[] = [
      {
        id: 'b-migrado',
        planificacionId: 'plan-1',
        carreraId: 'carr-1',
        periodoId: 'c1',
        nivel: 4,
        materiaId: 'mat-migrada',
        docenteId: 'doc-1',
        laboratorioId: 'lab-p2-migrado',
        diaSemana: 'MARTES',
        horaInicio: '09:30',
        horaFin: '11:30',
        estado: 'CONFIRMADA',
        observacion: null,
        version: 2,
      },
    ]
    vi.mocked(operational.obtenerMiHorario).mockResolvedValue(bloqueMigrado)

    render(<MiHorarioPage />)

    expect(await screen.findByText('Sistemas Operativos')).toBeInTheDocument()
    expect(screen.getByText(/L-P201/)).toBeInTheDocument()
    expect(screen.getByText(/Piso 2/)).toBeInTheDocument()
    expect(screen.queryByText(/Piso 1 · Planta Baja/)).not.toBeInTheDocument()
  })

  it('permite consultar laboratorios de su horario con piso formateado humanamente', async () => {
    vi.mocked(operational.obtenerMiHorario).mockResolvedValue([
      ...mockBloques,
      {
        id: 'b-2',
        planificacionId: 'plan-1',
        carreraId: 'carr-1',
        periodoId: 'c1',
        nivel: 4,
        materiaId: 'mat-migrada',
        docenteId: 'doc-1',
        laboratorioId: 'lab-p2-migrado',
        diaSemana: 'MARTES',
        horaInicio: '09:30',
        horaFin: '11:30',
        estado: 'CONFIRMADA',
        observacion: null,
        version: 1,
      },
    ])

    render(<StudentLaboratoriosPage />)

    expect(
      await screen.findByText('Laboratorios de mi horario'),
    ).toBeInTheDocument()
    expect(screen.getByText(/Lab Fundamentos/)).toBeInTheDocument()
    expect(screen.getByText('Piso 1 · Planta Baja')).toBeInTheDocument()
    expect(screen.getByText(/Lab Redes Nuevas/)).toBeInTheDocument()
    expect(screen.getByText('Piso 2')).toBeInTheDocument()
    // Verificar que piso 0 no desaparece ni dice "Piso 0"
    expect(screen.queryByText(/Piso 0/)).not.toBeInTheDocument()
  })

  it('cambiar ciclo histórico solo consulta y no modifica el contexto', async () => {
    render(<StudentHistorialPage />)
    await screen.findByText('Aplicaciones Web')
    fireEvent.change(screen.getByLabelText('Ciclo consultado'), {
      target: { value: 'c0' },
    })
    await waitFor(() =>
      expect(operational.obtenerMiHorario).toHaveBeenLastCalledWith('c0'),
    )
    expect(operational.historialAsistencia).toHaveBeenLastCalledWith('c0')
    expect(usuarios.obtenerMiContextoAcademico).not.toHaveBeenCalled()
  })

  it('sincroniza consulta de horario con el AcademicPeriodContext global', async () => {
    const periodoPPA = {
      id: 'c1',
      codigo: 'PPA',
      nombre: 'REGULAR 2026-2027 PPA',
      fechaInicio: '2026-04-01',
      fechaFin: '2026-08-31',
      estado: 'ACTIVO' as const,
    }
    const contextValue = {
      periodos: [periodoPPA],
      periodoVigente: periodoPPA,
      periodoSeleccionado: periodoPPA,
      seleccionarPeriodo: vi.fn(),
      cargando: false,
    }

    render(
      <AcademicPeriodContext.Provider value={contextValue}>
        <MiHorarioPage />
      </AcademicPeriodContext.Provider>,
    )

    await waitFor(() =>
      expect(operational.obtenerMiHorario).toHaveBeenCalledWith('c1'),
    )
  })

  it('presenta estados vacío y error sin romper la pantalla', async () => {
    vi.mocked(operational.obtenerMiHorario).mockResolvedValue([])
    render(<MiHorarioPage />)
    expect(
      await screen.findByText(
        'No existe un horario aprobado para el ciclo seleccionado.',
      ),
    ).toBeInTheDocument()
  })

  it('maneja de forma segura un estudiante todavía sin contexto', async () => {
    vi.mocked(usuarios.obtenerMisContextosAcademicos).mockResolvedValue([])
    vi.mocked(operational.obtenerMiHorario).mockResolvedValue([])
    render(<MiHorarioPage />)
    expect(
      await screen.findByText(
        'No existe un horario aprobado para el ciclo seleccionado.',
      ),
    ).toBeInTheDocument()
  })
})
