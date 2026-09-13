import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from './reservasApi'
import { ReservaDetailPage } from './ReservaDetailPage'

vi.mock('./reservasApi', async (original) => ({ ...(await original<typeof import('./reservasApi')>()), obtenerReservaPorId: vi.fn(), cancelarReserva: vi.fn() }))
vi.mock('../../auth', async (original) => ({ ...(await original<typeof import('../../auth')>()), useAuth: () => ({ usuario: { permisos: ['RESERVA_CANCELAR'] } }) }))
vi.mock('../../components/DashboardLayout', () => ({ DashboardLayout: ({ children }: { children: React.ReactNode }) => <>{children}</> }))
const reserva = { id: 'r1', solicitudId: 's1', laboratorioId: 'lab-1', responsableId: 'p1', fechaReserva: '2026-08-20', horaInicio: '08:00', horaFin: '10:00', estado: 'PROGRAMADA' as const, codigoReserva: 'RES-1', creadaEn: '', actualizadaEn: '', version: 0 }
function page() { render(<MemoryRouter initialEntries={['/reservas/r1']}><Routes><Route path="/reservas/:id" element={<ReservaDetailPage />} /></Routes></MemoryRouter>) }
describe('ReservaDetailPage', () => {
  beforeEach(() => vi.resetAllMocks())
  it('muestra detalle y cancelación para permiso válido', async () => { vi.mocked(api.obtenerReservaPorId).mockResolvedValue(reserva); page(); expect(await screen.findByText('RES-1')).toBeInTheDocument(); expect(screen.getByRole('button', { name: 'Cancelar reserva' })).toBeInTheDocument() })
  it('cancela una reserva programada', async () => { vi.spyOn(window, 'confirm').mockReturnValue(true); vi.mocked(api.obtenerReservaPorId).mockResolvedValue(reserva); vi.mocked(api.cancelarReserva).mockResolvedValue({ ...reserva, estado: 'CANCELADA' }); page(); fireEvent.change(await screen.findByLabelText('Motivo de cancelación'), { target: { value: 'Cambio' } }); fireEvent.click(screen.getByRole('button', { name: 'Cancelar reserva' })); expect(await screen.findByText('CANCELADA')).toBeInTheDocument() })
})
