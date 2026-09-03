import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as academico from '../../services/academicoApi'
import * as usuarios from '../../services/usuariosApi'
import * as operational from '../../services/operationalApi'
import * as reservas from '../reservas/reservasApi'
import { AdminDashboard } from './AdminDashboard'

vi.mock('../../services/academicoApi')
vi.mock('../../services/usuariosApi')
vi.mock('../../services/operationalApi')
vi.mock('../reservas/reservasApi')

describe('AdminDashboard', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(usuarios.listarPerfiles).mockResolvedValue([
      {
        id: 'p1',
        identificacion: '1',
        nombres: 'Ana',
        apellidos: 'Admin',
        emailInstitucional: 'ana@uteq.edu.ec',
        emailPersonal: null,
        telefono: null,
        direccion: null,
        fechaNacimiento: null,
        fotoUrl: null,
        activo: true,
        creadoEn: '',
        actualizadoEn: '',
      },
    ])
    vi.mocked(academico.obtenerDocentes).mockResolvedValue([
      { id: 'd1', perfilId: 'p2', codigoDocente: 'D1', activo: true },
    ])
    vi.mocked(usuarios.listarAdministradores).mockResolvedValue([])
    vi.mocked(academico.obtenerCarreras).mockResolvedValue([])
    vi.mocked(academico.obtenerPisos).mockResolvedValue([])
    vi.mocked(academico.obtenerEquipos).mockResolvedValue([])
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([
      {
        id: 'l1',
        pisoId: 'pi1',
        codigo: 'LAB-1',
        nombre: 'Software',
        capacidad: 20,
        descripcion: '',
        estado: 'DISPONIBLE',
        activo: true,
        creadoEn: '',
        actualizadoEn: '',
      },
    ])
    vi.mocked(reservas.obtenerReservas).mockResolvedValue([])
    vi.mocked(reservas.obtenerSolicitudes).mockResolvedValue([])
    vi.mocked(operational.listarIncidentes).mockResolvedValue([])
    vi.mocked(operational.listarPlanificaciones).mockResolvedValue([])
  })

  it('muestra métricas globales y accesos humanos sin UUID', async () => {
    render(
      <MemoryRouter>
        <AdminDashboard />
      </MemoryRouter>,
    )
    expect(await screen.findByText('Usuarios activos')).toBeInTheDocument()
    expect(screen.getByText('1 perfiles registrados')).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'Gestionar usuarios' }),
    ).toBeInTheDocument()
    expect(screen.queryByText('p1')).not.toBeInTheDocument()
  })

  it('presenta error controlado si falla la API', async () => {
    vi.mocked(usuarios.listarPerfiles).mockRejectedValue(new Error('caída'))
    render(
      <MemoryRouter>
        <AdminDashboard />
      </MemoryRouter>,
    )
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No fue posible cargar',
    )
  })
})
