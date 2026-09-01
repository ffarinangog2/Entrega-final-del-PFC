import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from './reservasApi'
import * as academico from '../../services/academicoApi'
import { ReservasListPage } from './ReservasListPage'

vi.mock('./reservasApi', async (original) => ({
  ...(await original<typeof import('./reservasApi')>()),
  obtenerReservas: vi.fn(),
  obtenerSolicitudes: vi.fn(),
}))
vi.mock('../../services/academicoApi')
vi.mock('../../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => (
    <>{children}</>
  ),
}))
vi.mock('../../auth', async (original) => ({
  ...(await original<typeof import('../../auth')>()),
  useAuth: () => ({ usuario: { roles: ['DOCENTE'] } }),
}))

const reserva = {
  id: 'r1',
  solicitudId: 's1',
  laboratorioId: 'lab-1',
  responsableId: 'p1',
  fechaReserva: '2026-08-20',
  horaInicio: '08:00',
  horaFin: '10:00',
  estado: 'PROGRAMADA' as const,
  codigoReserva: 'RES-1',
  creadaEn: '',
  actualizadaEn: '',
  version: 0,
}
const solicitud = {
  id: 's1',
  solicitanteId: 'p1',
  docenteId: 'd1',
  laboratorioId: 'lab-1',
  materiaId: 'm1',
  periodoLectivoId: 'pe1',
  fechaReserva: '2026-08-20',
  horaInicio: '08:00',
  horaFin: '10:00',
  numeroParticipantes: 20,
  motivo: 'Clase',
  observacion: '',
  estado: 'EN_REVISION' as const,
  propuestaFecha: null,
  propuestaHoraInicio: null,
  propuestaHoraFin: null,
  propuestaLaboratorioId: null,
  propuestaObservacion: null,
  reservaId: null,
  creadaEn: '',
  actualizadaEn: '',
  version: 0,
}

describe('ReservasListPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(api.obtenerReservas).mockResolvedValue([reserva])
    vi.mocked(api.obtenerSolicitudes).mockResolvedValue([solicitud])
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([
      {
        id: 'lab-1',
        pisoId: 'p1',
        codigo: 'LAB-A',
        nombre: 'Redes',
        capacidad: 20,
        descripcion: '',
        estado: 'DISPONIBLE',
        activo: true,
        creadoEn: '',
        actualizadoEn: '',
      },
    ])
    vi.mocked(academico.obtenerMaterias).mockResolvedValue([
      {
        id: 'm1',
        carreraId: 'c1',
        codigo: 'MAT-A',
        nombre: 'Redes I',
        numeroHoras: 40,
        activo: true,
      },
    ])
  })
  it('muestra solicitudes con nombres humanos', async () => {
    render(
      <MemoryRouter>
        <ReservasListPage />
      </MemoryRouter>,
    )
    expect(await screen.findByText('MAT-A — Redes I')).toBeInTheDocument()
    expect(screen.getByText('LAB-A — Redes')).toBeInTheDocument()
    expect(screen.getByText('En revisión')).toBeInTheDocument()
  })
  it('permite cambiar al tab de reservas', async () => {
    render(
      <MemoryRouter>
        <ReservasListPage />
      </MemoryRouter>,
    )
    await screen.findByText('MAT-A — Redes I')
    fireEvent.click(screen.getByRole('tab', { name: 'Reservas' }))
    expect(screen.getByText('RES-1')).toBeInTheDocument()
    expect(screen.getAllByText('Programada')).toHaveLength(2)
  })
  it('explica un 403 de administrador de piso', async () => {
    vi.mocked(api.obtenerSolicitudes).mockRejectedValue(
      new (await import('../../services/apiClient')).ApiError(
        403,
        'No tiene permisos',
      ),
    )
    render(
      <MemoryRouter>
        <ReservasListPage />
      </MemoryRouter>,
    )
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No tiene un piso operativo asignado',
    )
  })
})
