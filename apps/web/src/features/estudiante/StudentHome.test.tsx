import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as academico from '../../services/academicoApi'
import * as operational from '../../services/operationalApi'
import * as usuarios from '../../services/usuariosApi'
import { StudentHome } from './StudentHome'

vi.mock('../../services/academicoApi')
vi.mock('../../services/operationalApi')
vi.mock('../../services/usuariosApi')

describe('inicio del estudiante', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(usuarios.obtenerMiContextoAcademico).mockResolvedValue({
      id: 'ctx',
      estudianteId: 'e',
      carreraId: 'c',
      periodoId: 'p',
      nivel: 8,
      activo: true,
      creadoEn: '',
    })
    vi.mocked(academico.obtenerCarreras).mockResolvedValue([])
    vi.mocked(academico.obtenerPeriodos).mockResolvedValue([])
    vi.mocked(academico.obtenerMaterias).mockResolvedValue([])
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([])
    vi.mocked(academico.obtenerPisos).mockResolvedValue([])
    vi.mocked(operational.listarSesionesAbiertas).mockResolvedValue([])
    vi.mocked(operational.obtenerMiHorario).mockResolvedValue([])
  })

  it('presenta un estado vacío humano sin inventar actividad', async () => {
    render(
      <MemoryRouter>
        <StudentHome />
      </MemoryRouter>,
    )
    expect(await screen.findByText('Carrera institucional')).toBeInTheDocument()
    expect(
      screen.getByText('No existe un horario aprobado para tu contexto actual.'),
    ).toBeInTheDocument()
    expect(screen.queryByText(/Asistencia habilitada/)).not.toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: /Reportar incidencia/ }),
    ).toHaveAttribute('href', '/incidentes')
  })

  it('presenta el error controlado del contexto', async () => {
    vi.mocked(usuarios.obtenerMiContextoAcademico).mockRejectedValue(
      new Error('Contexto académico no disponible'),
    )
    render(
      <MemoryRouter>
        <StudentHome />
      </MemoryRouter>,
    )
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Contexto académico no disponible',
    )
  })

  it('formatea piso 0 como Piso 1 · Planta Baja sin perderlo por falsedad y no muestra Piso 0', async () => {
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([
      {
        id: 'l-pb',
        pisoId: 'p-cero',
        codigo: 'LAB-PB-01',
        nombre: 'Lab Redes',
        capacidad: 25,
        descripcion: '',
        estado: 'DISPONIBLE',
        activo: true,
        creadoEn: '',
        actualizadoEn: '',
      },
    ])
    vi.mocked(academico.obtenerPisos).mockResolvedValue([
      { id: 'p-cero', bloqueId: 'b1', numero: 0, descripcion: '', activo: true },
    ])
    vi.mocked(academico.obtenerMaterias).mockResolvedValue([
      {
        id: 'm1',
        carreraId: 'c',
        codigo: 'RED',
        nombre: 'Redes I',
        numeroHoras: 4,
        nivel: 8,
        activo: true,
      },
    ])
    vi.mocked(operational.obtenerMiHorario).mockResolvedValue([
      {
        id: 'bloque-pb',
        planificacionId: 'plan-1',
        nivel: 8,
        periodoId: 'p',
        carreraId: 'c',
        materiaId: 'm1',
        docenteId: '',
        laboratorioId: 'l-pb',
        diaSemana: 'LUNES',
        horaInicio: '08:00',
        horaFin: '10:00',
        estado: 'CONFIRMADA' as const,
        observacion: null,
        version: 1,
      },
    ])

    render(
      <MemoryRouter>
        <StudentHome />
      </MemoryRouter>,
    )
    expect(await screen.findByText('Redes I')).toBeInTheDocument()
    expect(screen.getByText(/LAB-PB-01 · Piso 1 · Planta Baja/)).toBeInTheDocument()
    expect(screen.queryByText(/Piso 0/)).not.toBeInTheDocument()
  })
})
