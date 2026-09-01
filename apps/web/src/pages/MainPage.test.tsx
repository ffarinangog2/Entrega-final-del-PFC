import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as academico from '../services/academicoApi'
import { MainPage } from './MainPage'

let usuario = { perfilId: 'perfil-1', roles: ['DOCENTE'], permisos: [] }

vi.mock('../auth', async (original) => ({
  ...(await original<typeof import('../auth')>()),
  useAuth: () => ({ usuario }),
}))
vi.mock('../services/academicoApi')
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
})
