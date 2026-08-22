import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as reservasApi from './reservasApi'
import { ReservaDetailPage } from './ReservaDetailPage'
import type { Reserva } from './reservasApi'

vi.mock('./reservasApi', async (importOriginal) => ({
  ...(await importOriginal<typeof import('./reservasApi')>()),
  obtenerReservas: vi.fn(),
  obtenerReservaPorId: vi.fn(),
  cancelarReserva: vi.fn(),
}))
vi.mock('../../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

const reservaMock: Reserva = {
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

function renderDetail() {
  render(<MemoryRouter initialEntries={[`/reservas/${reservaMock.id}`]}><Routes><Route path="/reservas/:id" element={<ReservaDetailPage />} /></Routes></MemoryRouter>)
}

describe('ReservaDetailPage', () => {
  beforeEach(() => vi.resetAllMocks())

  it('abre y muestra el detalle de una reserva', async () => {
    vi.mocked(reservasApi.obtenerReservaPorId).mockResolvedValue(reservaMock)
    renderDetail()
    expect(await screen.findByRole('heading', { name: 'RES-2026-0001' })).toBeInTheDocument()
    expect(screen.getByText(reservaMock.solicitudId)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Volver al listado/ })).toHaveAttribute('href', '/reservas')
  })

  it('distingue un detalle inexistente con respuesta 404', async () => {
    vi.mocked(reservasApi.obtenerReservaPorId).mockRejectedValue(new reservasApi.ReservasApiError(404, 'No encontrado'))
    renderDetail()
    expect(await screen.findByRole('alert')).toHaveTextContent('La reserva solicitada no existe.')
  })

  it('cancela una reserva programada exitosamente', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(reservasApi.obtenerReservaPorId).mockResolvedValue(reservaMock)
    vi.mocked(reservasApi.cancelarReserva).mockResolvedValue({ ...reservaMock, estado: 'CANCELADA' })
    renderDetail()
    const input = await screen.findByLabelText('Motivo de cancelación')
    input.dispatchEvent(new Event('input', { bubbles: true }))
    await import('@testing-library/react').then(({ fireEvent }) => fireEvent.change(input, { target: { value: 'Cambio de horario' } }))
    await import('@testing-library/react').then(({ fireEvent }) => fireEvent.click(screen.getByRole('button', { name: 'Cancelar reserva' })))
    expect(await screen.findByText('CANCELADA')).toBeInTheDocument()
    expect(reservasApi.cancelarReserva).toHaveBeenCalledWith(reservaMock.id, 'Cambio de horario')
  })

  it('muestra el error al cancelar', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(reservasApi.obtenerReservaPorId).mockResolvedValue(reservaMock)
    vi.mocked(reservasApi.cancelarReserva).mockRejectedValue(new Error('Cancelación rechazada'))
    renderDetail()
    const { fireEvent } = await import('@testing-library/react')
    fireEvent.change(await screen.findByLabelText('Motivo de cancelación'), { target: { value: 'Motivo válido' } })
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar reserva' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Cancelación rechazada')
  })
})
