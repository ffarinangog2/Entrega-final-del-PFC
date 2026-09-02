import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as academico from '../services/academicoApi'
import * as operational from '../services/operationalApi'
import { MainPage } from './MainPage'

let usuario = { perfilId: 'perfil-1', roles: ['DOCENTE'], permisos: [] }

vi.mock('../auth', async (original) => ({
  ...(await original<typeof import('../auth')>()),
  useAuth: () => ({ usuario }),
}))
vi.mock('../services/academicoApi')
vi.mock('../services/operationalApi')
vi.mock('../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => (
    <>{children}</>
  ),
}))
vi.mock('../features/laboratorios/LaboratoriosPanel', () => ({
  LaboratoriosPanel: () => <div>Laboratorios globales</div>,
}))
vi.mock('../features/monitoreo/MonitoreoPanel', () => ({
  MonitoreoPanel: () => <div>Monitoreo global</div>,
}))
vi.mock('../features/admin/AdminDashboard', () => ({
  AdminDashboard: () => <div>Resumen administrativo global</div>,
}))

describe('MainPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    usuario = { perfilId: 'perfil-1', roles: ['DOCENTE'], permisos: [] }
    vi.mocked(academico.obtenerDocentePorPerfil).mockResolvedValue({
      id: 'doc-1',
      perfilId: 'perfil-1',
      codigoDocente: 'DOC-1',
      activo: true,
    })
    vi.mocked(academico.obtenerHorariosDocente).mockResolvedValue([
      {
        id: 'h-1',
        materiaId: 'm-1',
        periodoLectivoId: 'p-1',
        laboratorioId: 'l-1',
        docenteId: 'doc-1',
        diaSemana: 'LUNES',
        horaInicio: '07:30',
        horaFin: '09:30',
        paralelo: 'A',
        activo: true,
      },
    ])
    vi.mocked(academico.obtenerMaterias).mockResolvedValue([
      {
        id: 'm-1',
        carreraId: 'c-1',
        codigo: 'MAT-1',
        nombre: 'Programación I',
        numeroHoras: 64,
        activo: true,
      },
    ])
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([
      {
        id: 'l-1',
        pisoId: 'piso-1',
        codigo: 'LAB-1',
        nombre: 'Laboratorio de Software',
        capacidad: 30,
        descripcion: '',
        estado: 'DISPONIBLE',
        activo: true,
        creadoEn: '',
        actualizadoEn: '',
      },
    ])
  })

  it('muestra al docente únicamente su horario con nombres humanos', async () => {
    render(
      <MemoryRouter>
        <MainPage />
      </MemoryRouter>,
    )
    expect(await screen.findByText('Programación I')).toBeInTheDocument()
    expect(screen.getByText('Laboratorio de Software')).toBeInTheDocument()
    expect(academico.obtenerDocentePorPerfil).toHaveBeenCalledWith('perfil-1')
  })

  it('destaca las clases de hoy sin permitir editar la planificación base', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.setSystemTime(new Date('2026-09-07T08:00:00'))
    render(
      <MemoryRouter>
        <MainPage />
      </MemoryRouter>,
    )
    expect(await screen.findByRole('heading', { name: 'Hoy' })).toBeInTheDocument()
    expect(screen.getByText('Programada')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Editar/ })).not.toBeInTheDocument()
    vi.useRealTimers()
  })

  it('no consulta catálogos globales para un estudiante', () => {
    usuario = { perfilId: 'perfil-2', roles: ['ESTUDIANTE'], permisos: [] }
    render(
      <MemoryRouter>
        <MainPage />
      </MemoryRouter>,
    )
    expect(
      screen.getByRole('heading', { name: 'Mi información académica' }),
    ).toBeInTheDocument()
    expect(academico.obtenerLaboratorios).not.toHaveBeenCalled()
  })

  it('muestra al coordinador carrera, periodo y estado de planificación', async () => {
    usuario = { perfilId: 'perfil-3', roles: ['COORDINADOR'], permisos: [] }
    vi.mocked(operational.listarPlanificaciones).mockResolvedValue([
      {
        id: 'p-1',
        periodoId: 'periodo-1',
        carreraId: 'c-1',
        materiaId: 'm-1',
        docenteId: 'd-1',
        laboratorioId: 'l-1',
        diaSemana: 'LUNES',
        horaInicio: '07:30',
        horaFin: '09:30',
        estado: 'ENVIADA',
        observacion: null,
        version: 0,
      },
    ])
    vi.mocked(academico.obtenerPeriodoActual).mockResolvedValue({
      id: 'periodo-1',
      codigo: '2026-B',
      nombre: 'Periodo 2026-B',
      fechaInicio: '',
      fechaFin: '',
      estado: 'ACTIVO',
    })
    vi.mocked(academico.obtenerCarreras).mockResolvedValue([
      {
        id: 'c-1',
        facultadId: 'f-1',
        codigo: 'IS',
        nombre: 'Ingeniería de Software',
        activo: true,
      },
    ])
    vi.mocked(academico.obtenerDocentes).mockResolvedValue([])
    render(
      <MemoryRouter>
        <MainPage />
      </MemoryRouter>,
    )
    expect(
      (await screen.findByText(/Carrera:/)).closest('p'),
    ).toHaveTextContent('Ingeniería de Software')
    expect(screen.getByText(/Periodo:/).closest('p')).toHaveTextContent(
      'Periodo 2026-B',
    )
    expect(screen.getByText(/Estado:/).closest('p')).toHaveTextContent(
      'En revisión',
    )
  })
})
