import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as reservasApi from './reservasApi'
import { ReservasListPage } from './ReservasListPage'
import type { Reserva } from './reservasApi'

vi.mock('./reservasApi', async (importOriginal) => ({
  ...(await importOriginal<typeof import('./reservasApi')>()),
  obtenerReservas: vi.fn(),
  obtenerReservaPorId: vi.fn(),
}))
vi.mock('../../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

export const reservaMock: Reserva = {
  id: '11111111-1111-1111-1111-111111111111',
  solicitudId: '22222222-2222-2222-2222-222222222222',
  laboratorioId: '33333333-3333-3333-3333-333333333333',
  responsableId: '44444444-4444-4444-4444-444444444444',
  fechaReserva: '2026-08-20',
  horaInicio: '08:00:00',
  horaFin: '10:00:00',
  estado: 'PROGRAMADA',
  codigoReserva: 'RES-2026-0001',
  creadaEn: '2026-08-18T10:00:00Z',
  actualizadaEn: '2026-08-18T10:00:00Z',
  version: 0,
}

function renderPage() {
  render(<MemoryRouter><ReservasListPage /></MemoryRouter>)
}

describe('ReservasListPage', () => {
  beforeEach(() => vi.resetAllMocks())

  it('muestra loading mientras consulta el listado', () => {
    vi.mocked(reservasApi.obtenerReservas).mockReturnValue(new Promise(() => {}))
    renderPage()
    expect(screen.getByText('Cargando reservas...')).toBeInTheDocument()
  })

  it('muestra las reservas reales y el enlace al detalle', async () => {
    vi.mocked(reservasApi.obtenerReservas).mockResolvedValue([reservaMock])
    renderPage()
    expect(await screen.findByText('RES-2026-0001')).toBeInTheDocument()
    expect(screen.getByText('33333333-3333-3333-3333-333333333333')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver detalle' })).toHaveAttribute('href', `/reservas/${reservaMock.id}`)
  })

  it('muestra el estado vacío', async () => {
    vi.mocked(reservasApi.obtenerReservas).mockResolvedValue([])
    renderPage()
    expect(await screen.findByText('No hay reservas registradas.')).toBeInTheDocument()
  })

  it('muestra el error del listado', async () => {
    vi.mocked(reservasApi.obtenerReservas).mockRejectedValue(new Error('Gateway no disponible'))
    renderPage()
    expect(await screen.findByRole('alert')).toHaveTextContent('Gateway no disponible')
    expect(screen.getByRole('button', { name: 'Intentar de nuevo' })).toBeInTheDocument()
  })
})
