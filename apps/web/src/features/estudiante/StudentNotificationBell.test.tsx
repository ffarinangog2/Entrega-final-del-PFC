import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '../../services/operationalApi'
import { StudentNotificationBell } from './StudentNotificationBell'

vi.mock('../../services/operationalApi', () => ({
  listarNotificaciones: vi.fn(),
  obtenerNotificacionesNoLeidas: vi.fn(),
  marcarNotificacionLeida: vi.fn(),
}))

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

describe('StudentNotificationBell', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockNavigate.mockReset()
  })

  it('1. usuario sin notificaciones -> muestra estado vacío con "No hay notificaciones pendientes"', async () => {
    vi.mocked(api.listarNotificaciones).mockResolvedValue([])
    vi.mocked(api.obtenerNotificacionesNoLeidas).mockResolvedValue({ cantidad: 0 })
    render(
      <MemoryRouter>
        <StudentNotificationBell />
      </MemoryRouter>,
    )

    const trigger = await screen.findByRole('button', { name: 'Notificaciones: 0 pendientes' })
    expect(trigger).toBeInTheDocument()
    expect(screen.queryByText(/^[0-9]+$/)).not.toBeInTheDocument()

    fireEvent.click(trigger)
    expect(await screen.findByText('No hay notificaciones pendientes')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver todas las notificaciones' })).toHaveAttribute(
      'href',
      '/notificaciones',
    )
  })

  it('2. badge refleja solo no leídas sin sumar otras métricas', async () => {
    vi.mocked(api.listarNotificaciones).mockResolvedValue([
      { id: 'n1', titulo: 'Aviso 1', cuerpo: 'C1', tipo: 'PLANIFICACION', referenciaId: null, leida: false, creadaEn: '2026-09-06T10:00:00Z' },
      { id: 'n2', titulo: 'Aviso 2', cuerpo: 'C2', tipo: 'SOLICITUD', referenciaId: 'sol-1', leida: false, creadaEn: '2026-09-06T11:00:00Z' },
      { id: 'n3', titulo: 'Aviso 3', cuerpo: 'C3', tipo: 'RESERVA', referenciaId: 'res-1', leida: true, creadaEn: '2026-09-06T09:00:00Z' },
    ])
    vi.mocked(api.obtenerNotificacionesNoLeidas).mockResolvedValue({ cantidad: 2 })

    render(
      <MemoryRouter>
        <StudentNotificationBell />
      </MemoryRouter>,
    )

    // Solo 2 no leídas de 3 totales
    const trigger = await screen.findByRole('button', { name: 'Notificaciones: 2 pendientes' })
    expect(trigger).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
  })

  it('3. popup muestra exactamente las 5 no leídas más recientes y badge refleja el total real', async () => {
    // 7 notificaciones no leídas con fechas distintas para comprobar orden y límite
    const sieteNoLeidas: api.NotificacionInterna[] = [
      { id: 'item-1', titulo: 'Notificación 1 (más antigua)', cuerpo: 'Detalle 1', tipo: 'SOLICITUD', referenciaId: 'ref-1', leida: false, creadaEn: '2026-09-06T10:00:00Z' },
      { id: 'item-2', titulo: 'Notificación 2 (antigua)', cuerpo: 'Detalle 2', tipo: 'SOLICITUD', referenciaId: 'ref-2', leida: false, creadaEn: '2026-09-06T11:00:00Z' },
      { id: 'item-3', titulo: 'Notificación 3', cuerpo: 'Detalle 3', tipo: 'SOLICITUD', referenciaId: 'ref-3', leida: false, creadaEn: '2026-09-06T12:00:00Z' },
      { id: 'item-4', titulo: 'Notificación 4', cuerpo: 'Detalle 4', tipo: 'SOLICITUD', referenciaId: 'ref-4', leida: false, creadaEn: '2026-09-06T13:00:00Z' },
      { id: 'item-5', titulo: 'Notificación 5', cuerpo: 'Detalle 5', tipo: 'SOLICITUD', referenciaId: 'ref-5', leida: false, creadaEn: '2026-09-06T14:00:00Z' },
      { id: 'item-6', titulo: 'Notificación 6', cuerpo: 'Detalle 6', tipo: 'SOLICITUD', referenciaId: 'ref-6', leida: false, creadaEn: '2026-09-06T15:00:00Z' },
      { id: 'item-7', titulo: 'Notificación 7 (más reciente)', cuerpo: 'Detalle 7', tipo: 'SOLICITUD', referenciaId: 'ref-7', leida: false, creadaEn: '2026-09-06T16:00:00Z' },
    ]

    vi.mocked(api.listarNotificaciones).mockResolvedValue(sieteNoLeidas)
    // El backend reporta 18 no leídas en total en la BD
    vi.mocked(api.obtenerNotificacionesNoLeidas).mockResolvedValue({ cantidad: 18 })

    render(
      <MemoryRouter>
        <StudentNotificationBell />
      </MemoryRouter>,
    )

    // El badge refleja el total real del usuario (18) y no solo las 5 visibles
    const trigger = await screen.findByRole('button', { name: 'Notificaciones: 18 pendientes' })
    expect(trigger).toBeInTheDocument()
    expect(screen.getByText('18')).toBeInTheDocument()

    fireEvent.click(trigger)

    // Popup muestra exactamente las 5 más recientes (7, 6, 5, 4, 3)
    expect(await screen.findByText('Notificación 7 (más reciente)')).toBeInTheDocument()
    expect(screen.getByText('Notificación 6')).toBeInTheDocument()
    expect(screen.getByText('Notificación 5')).toBeInTheDocument()
    expect(screen.getByText('Notificación 4')).toBeInTheDocument()
    expect(screen.getByText('Notificación 3')).toBeInTheDocument()

    // Las más antiguas (1 y 2) quedan excluidas de las 5 del popup
    expect(screen.queryByText('Notificación 1 (más antigua)')).not.toBeInTheDocument()
    expect(screen.queryByText('Notificación 2 (antigua)')).not.toBeInTheDocument()
  })

  it('4. una notificación leída no aparece en el popup de pendientes', async () => {
    vi.mocked(api.listarNotificaciones).mockResolvedValue([
      { id: 'leida-1', titulo: 'Ya fue leída', cuerpo: 'Desc', tipo: 'PLANIFICACION', referenciaId: null, leida: true, creadaEn: '2026-09-06T08:00:00Z' },
      { id: 'pendiente-1', titulo: 'Por leer', cuerpo: 'Desc', tipo: 'PLANIFICACION', referenciaId: null, leida: false, creadaEn: '2026-09-06T09:00:00Z' },
    ])
    vi.mocked(api.obtenerNotificacionesNoLeidas).mockResolvedValue({ cantidad: 1 })

    render(
      <MemoryRouter>
        <StudentNotificationBell />
      </MemoryRouter>,
    )

    const trigger = await screen.findByRole('button', { name: 'Notificaciones: 1 pendientes' })
    fireEvent.click(trigger)

    expect(await screen.findByText('Por leer')).toBeInTheDocument()
    expect(screen.queryByText('Ya fue leída')).not.toBeInTheDocument()
  })

  it('5, 6, 7 y 11. click en notificación: marca como leída en backend, disminuye badge, la quita del popup y navega', async () => {
    vi.mocked(api.listarNotificaciones).mockResolvedValue([
      { id: 'notif-1', titulo: 'Nueva solicitud', cuerpo: 'Detalle', tipo: 'SOLICITUD', referenciaId: 'sol-abc', leida: false, creadaEn: '2026-09-06T10:00:00Z' },
    ])
    vi.mocked(api.obtenerNotificacionesNoLeidas).mockResolvedValue({ cantidad: 1 })
    vi.mocked(api.marcarNotificacionLeida).mockResolvedValue({
      id: 'notif-1',
      titulo: 'Nueva solicitud',
      cuerpo: 'Detalle',
      tipo: 'SOLICITUD',
      referenciaId: 'sol-abc',
      leida: true,
      creadaEn: '2026-09-06T10:00:00Z',
    })

    render(
      <MemoryRouter>
        <StudentNotificationBell />
      </MemoryRouter>,
    )

    const trigger = await screen.findByRole('button', { name: 'Notificaciones: 1 pendientes' })
    fireEvent.click(trigger)

    const itemBtn = await screen.findByRole('button', { name: /Nueva solicitud/ })
    fireEvent.click(itemBtn)

    // 5. llama a marcarNotificacionLeida con el id
    await waitFor(() => {
      expect(api.marcarNotificacionLeida).toHaveBeenCalledWith('notif-1')
      // 11. navega al destino seguro contextual /solicitudes/sol-abc
      expect(mockNavigate).toHaveBeenCalledWith('/solicitudes/sol-abc')
      // 6. disminuye badge a 0
      expect(trigger).toHaveAttribute('aria-label', 'Notificaciones: 0 pendientes')
    })
  })

  it('12. tipo no resoluble navega a fallback /notificaciones', async () => {
    vi.mocked(api.listarNotificaciones).mockResolvedValue([
      { id: 'notif-desc', titulo: 'Desconocido', cuerpo: 'Info', tipo: 'DESCONOCIDO', referenciaId: 'xyz', leida: false, creadaEn: '2026-09-06T10:00:00Z' },
    ])
    vi.mocked(api.obtenerNotificacionesNoLeidas).mockResolvedValue({ cantidad: 1 })
    vi.mocked(api.marcarNotificacionLeida).mockResolvedValue({
      id: 'notif-desc',
      titulo: 'Desconocido',
      cuerpo: 'Info',
      tipo: 'DESCONOCIDO',
      referenciaId: 'xyz',
      leida: true,
      creadaEn: '2026-09-06T10:00:00Z',
    })

    render(
      <MemoryRouter>
        <StudentNotificationBell />
      </MemoryRouter>,
    )

    const trigger = await screen.findByRole('button', { name: 'Notificaciones: 1 pendientes' })
    fireEvent.click(trigger)

    const itemBtn = await screen.findByRole('button', { name: /Desconocido/ })
    fireEvent.click(itemBtn)

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/notificaciones')
    })
  })

  it('15. actualiza el contador al recuperar foco sin duplicar llamadas erróneas', async () => {
    vi.mocked(api.listarNotificaciones)
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([
        { id: 'n-foco', titulo: 'Foco nuevo', cuerpo: 'Cuerpo', tipo: 'PLANIFICACION', referenciaId: null, leida: false, creadaEn: '2026-09-06T10:00:00Z' },
      ])
    vi.mocked(api.obtenerNotificacionesNoLeidas)
      .mockResolvedValueOnce({ cantidad: 0 })
      .mockResolvedValueOnce({ cantidad: 1 })

    render(
      <MemoryRouter>
        <StudentNotificationBell />
      </MemoryRouter>,
    )

    const trigger = await screen.findByRole('button', { name: 'Notificaciones: 0 pendientes' })
    expect(trigger).toBeInTheDocument()

    fireEvent(window, new Event('focus'))

    expect(await screen.findByRole('button', { name: 'Notificaciones: 1 pendientes' })).toBeInTheDocument()
  })

  it('actualiza el contador ante evento notificaciones-actualizadas', async () => {
    vi.mocked(api.listarNotificaciones)
      .mockResolvedValueOnce([
        { id: 'n1', titulo: 'Aviso', cuerpo: 'Cuerpo', tipo: 'PLANIFICACION', referenciaId: null, leida: false, creadaEn: '2026-09-06T10:00:00Z' },
      ])
      .mockResolvedValueOnce([])
    vi.mocked(api.obtenerNotificacionesNoLeidas)
      .mockResolvedValueOnce({ cantidad: 1 })
      .mockResolvedValueOnce({ cantidad: 0 })

    render(
      <MemoryRouter>
        <StudentNotificationBell />
      </MemoryRouter>,
    )

    await screen.findByRole('button', { name: 'Notificaciones: 1 pendientes' })

    fireEvent(window, new Event('notificaciones-actualizadas'))

    expect(await screen.findByRole('button', { name: 'Notificaciones: 0 pendientes' })).toBeInTheDocument()
  })
})
