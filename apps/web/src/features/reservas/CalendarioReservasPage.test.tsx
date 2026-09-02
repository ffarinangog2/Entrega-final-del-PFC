import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from './reservasApi'
import { CalendarioReservasPage } from './CalendarioReservasPage'
import * as academico from '../../services/academicoApi'

let roles: string[] = ['DOCENTE']
vi.mock('../../auth', async (original) => ({
  ...(await original<typeof import('../../auth')>()),
  useAuth: () => ({ usuario: { roles } }),
}))

vi.mock('./reservasApi', async (original) => ({
  ...(await original<typeof import('./reservasApi')>()),
  obtenerCalendario: vi.fn(),
}))
vi.mock('../../services/academicoApi')
vi.mock('../../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => (
    <>{children}</>
  ),
}))
const reserva: api.Reserva = {
  id: 'r1',
  solicitudId: 's1',
  laboratorioId: 'lab-1',
  responsableId: 'p1',
  fechaReserva: '',
  horaInicio: '08:00:00',
  horaFin: '10:00:00',
  estado: 'PROGRAMADA',
  codigoReserva: 'RES-1',
  creadaEn: '',
  actualizadaEn: '',
  version: 0,
}
describe('CalendarioReservasPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    roles = ['DOCENTE']
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
  })
  it('muestra loading', () => {
    vi.mocked(api.obtenerCalendario).mockReturnValue(new Promise(() => {}))
    render(
      <MemoryRouter>
        <CalendarioReservasPage />
      </MemoryRouter>,
    )
    expect(screen.getByText('Cargando calendario...')).toBeInTheDocument()
  })
  it('muestra una semana con reservas', async () => {
    vi.mocked(api.obtenerCalendario).mockImplementation(async (desde) => [
      { ...reserva, fechaReserva: desde },
    ])
    render(
      <MemoryRouter>
        <CalendarioReservasPage />
      </MemoryRouter>,
    )
    expect(await screen.findByText(/RES-1/)).toBeInTheDocument()
    expect(await screen.findByText('LAB-A — Redes')).toBeInTheDocument()
  })
  it('muestra una semana vacía', async () => {
    vi.mocked(api.obtenerCalendario).mockResolvedValue([])
    render(
      <MemoryRouter>
        <CalendarioReservasPage />
      </MemoryRouter>,
    )
    expect(
      await screen.findByText('No hay reservas esta semana.'),
    ).toBeInTheDocument()
  })
  it('muestra error', async () => {
    vi.mocked(api.obtenerCalendario).mockRejectedValue(
      new Error('Calendario no disponible'),
    )
    render(
      <MemoryRouter>
        <CalendarioReservasPage />
      </MemoryRouter>,
    )
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Calendario no disponible',
    )
  })
  it('navega a la semana siguiente', async () => {
    vi.mocked(api.obtenerCalendario).mockResolvedValue([])
    render(
      <MemoryRouter>
        <CalendarioReservasPage />
      </MemoryRouter>,
    )
    await screen.findByText('No hay reservas esta semana.')
    fireEvent.click(screen.getByRole('button', { name: 'Semana siguiente' }))
    expect(api.obtenerCalendario).toHaveBeenCalledTimes(2)
  })
  it('coordinador consulta disponibilidad sin llamar reservas', async () => {
    roles = ['COORDINADOR']
    render(
      <MemoryRouter>
        <CalendarioReservasPage />
      </MemoryRouter>,
    )
    expect(await screen.findByText('LAB-A — Redes')).toBeInTheDocument()
    expect(api.obtenerCalendario).not.toHaveBeenCalled()
  })
})
