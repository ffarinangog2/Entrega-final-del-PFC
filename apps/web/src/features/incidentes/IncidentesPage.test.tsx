import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as academico from '../../services/academicoApi'
import * as operational from '../../services/operationalApi'
import { IncidentesPage } from './IncidentesPage'

let mockUsuario = {
  roles: ['ADMINISTRADOR'],
  permisos: ['INCIDENTE_CREAR', 'INCIDENTE_GESTIONAR'],
}

vi.mock('../../auth', () => ({
  useAuth: () => ({
    usuario: mockUsuario,
  }),
  hasPermission: (usuario: { permisos?: string[] }, perm: string) =>
    usuario?.permisos?.includes(perm) ?? false,
  hasAnyPermission: (usuario: { permisos?: string[] }, perms: string[]) =>
    perms.some((p) => usuario?.permisos?.includes(p)) ?? false,
  hasRole: (usuario: { roles?: string[] }, role: string) =>
    usuario?.roles?.includes(role) ?? false,
}))
vi.mock('../../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => (
    <>{children}</>
  ),
}))
vi.mock('../../services/operationalApi')
vi.mock('../../services/academicoApi')

describe('IncidentesPage institucional', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    mockUsuario = {
      roles: ['ADMINISTRADOR'],
      permisos: ['INCIDENTE_CREAR', 'INCIDENTE_GESTIONAR'],
    }
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([
      {
        id: 'lab-1',
        pisoId: 'p1',
        codigo: 'LAB-01',
        nombre: 'Laboratorio de Redes',
        capacidad: 30,
        descripcion: '',
        estado: 'DISPONIBLE',
        activo: true,
        creadoEn: '',
        actualizadoEn: '',
      },
    ])
    vi.mocked(operational.listarIncidentes).mockResolvedValue([
      {
        id: 'i-1',
        laboratorioEquipo: 'LAB-01',
        descripcion: 'Sin red',
        prioridad: 'ALTA',
        fecha: '2026-09-07',
        estado: 'REPORTADO',
        creadoEn: '',
      },
    ])
    vi.mocked(operational.crearIncidente).mockResolvedValue({
      id: 'i-2',
      laboratorioEquipo: 'LAB-01',
      descripcion: 'Cable suelto',
      prioridad: 'MEDIA',
      fecha: '2026-09-07',
      estado: 'REPORTADO',
      creadoEn: '',
    })
    vi.mocked(operational.actualizarIncidente).mockResolvedValue({
      id: 'i-1',
      laboratorioEquipo: 'LAB-01',
      descripcion: 'Sin red',
      prioridad: 'ALTA',
      fecha: '2026-09-07',
      estado: 'EN_REVISION',
      creadoEn: '',
    })
  })

  it('usa laboratorio institucional y permite filtros globales para gestor', async () => {
    render(<IncidentesPage />)
    expect(await screen.findByText('Sin red')).toBeInTheDocument()
    expect(screen.getByLabelText('Laboratorio o equipo').tagName).toBe('SELECT')
    expect(screen.getByLabelText('Estado')).toBeInTheDocument()

    // Cambiar estado como gestor
    fireEvent.change(screen.getByLabelText(/Estado de LAB-01/), {
      target: { value: 'EN_REVISION' },
    })
    await waitFor(() => {
      expect(operational.actualizarIncidente).toHaveBeenCalledWith(
        'i-1',
        'EN_REVISION',
      )
    })

    fireEvent.change(screen.getByLabelText('Laboratorio'), {
      target: { value: 'LAB-01' },
    })
    fireEvent.change(screen.getAllByLabelText('Prioridad')[1], {
      target: { value: 'ALTA' },
    })
    fireEvent.change(screen.getByLabelText('Estado'), {
      target: { value: 'REPORTADO' },
    })
    expect(screen.getByText('Sin red')).toBeInTheDocument()
    fireEvent.change(screen.getAllByLabelText('Prioridad')[1], {
      target: { value: 'BAJA' },
    })
    expect(screen.getByText('No existen incidentes.')).toBeInTheDocument()
  })
})

describe('IncidentesPage para ESTUDIANTE', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockUsuario = {
      roles: ['ESTUDIANTE'],
      permisos: ['INCIDENTE_CREAR', 'INCIDENTE_LEER'],
    }
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([])
    vi.mocked(operational.listarIncidentes).mockResolvedValue([
      {
        id: 'i-est-1',
        laboratorioEquipo: 'LAB-02',
        descripcion: 'Teclado dañado',
        prioridad: 'MEDIA',
        fecha: '2026-09-07',
        estado: 'REPORTADO',
        creadoEn: '',
      },
    ])
    vi.mocked(operational.crearIncidente).mockResolvedValue({
      id: 'i-nuevo',
      laboratorioEquipo: 'LAB-02',
      descripcion: 'Mouse roto',
      prioridad: 'BAJA',
      fecha: '2026-09-07',
      estado: 'REPORTADO',
      creadoEn: '',
    })
  })

  it('permite reportar incidente pero NO cambiar estado ni ver filtros globales', async () => {
    render(<IncidentesPage />)
    expect(await screen.findByText('Teclado dañado')).toBeInTheDocument()
    expect(screen.getByLabelText('Laboratorio o equipo').tagName).toBe('INPUT')
    expect(screen.queryByLabelText('Estado')).not.toBeInTheDocument()
    expect(
      screen.queryByLabelText(/Estado de LAB-02/),
    ).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Laboratorio o equipo'), {
      target: { value: 'LAB-02' },
    })
    fireEvent.change(screen.getByLabelText('Descripción'), {
      target: { value: 'Mouse roto' },
    })
    fireEvent.click(screen.getByRole('button', { name: /Reportar incidente/ }))

    await waitFor(() => {
      expect(operational.crearIncidente).toHaveBeenCalledWith(
        expect.objectContaining({
          laboratorioEquipo: 'LAB-02',
          descripcion: 'Mouse roto',
        }),
      )
    })
  })

  it('estudiante no tiene controles de transición de estado y actualizarIncidente jamás es invocado', async () => {
    render(<IncidentesPage />)
    expect(await screen.findByText('Teclado dañado')).toBeInTheDocument()
    expect(screen.queryByLabelText(/Estado de/)).not.toBeInTheDocument()
    expect(operational.actualizarIncidente).not.toHaveBeenCalled()
  })
})
