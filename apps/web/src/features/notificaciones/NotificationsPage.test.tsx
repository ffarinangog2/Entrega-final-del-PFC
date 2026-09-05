import type { ReactNode } from 'react'
import { render, screen, fireEvent } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { NotificationsPage } from './NotificationsPage'
import * as api from '../../services/operationalApi'

vi.mock('../../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: ReactNode }) => <>{children}</>,
}))

vi.mock('../../services/operationalApi', () => ({
  listarNotificaciones: vi.fn(),
  marcarNotificacionLeida: vi.fn(),
  marcarTodasNotificacionesLeidas: vi.fn(),
}))

describe('NotificationsPage', () => {
  beforeEach(() => {
    vi.mocked(api.listarNotificaciones).mockResolvedValue([
      {
        id: 'n1',
        titulo: 'Planificación aprobada',
        cuerpo: 'Horario disponible',
        tipo: 'PLAN',
        referenciaId: null,
        leida: false,
        creadaEn: '2026-09-05T10:00:00Z',
      },
      {
        id: 'n2',
        titulo: 'Sesión finalizada',
        cuerpo: 'Clase registrada',
        tipo: 'ASIST',
        referenciaId: null,
        leida: true,
        creadaEn: '2026-09-05T09:00:00Z',
      },
    ])
    vi.mocked(api.marcarNotificacionLeida).mockResolvedValue({
      id: 'n1',
      titulo: 'Planificación aprobada',
      cuerpo: 'Horario disponible',
      tipo: 'PLAN',
      referenciaId: null,
      leida: true,
      creadaEn: '2026-09-05T10:00:00Z',
    })
    vi.mocked(api.marcarTodasNotificacionesLeidas).mockResolvedValue(undefined)
  })

  it('lista y permite marcar una notificación propia como leída', async () => {
    render(<NotificationsPage />)
    expect(await screen.findByText('Planificación aprobada')).toBeInTheDocument()
    expect(screen.getByText('Sesión finalizada')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Marcar como leída' }))
    expect(api.marcarNotificacionLeida).toHaveBeenCalledWith('n1')
  })

  it('permite filtrar solo no leídas y marcar todas como leídas', async () => {
    render(<NotificationsPage />)
    expect(await screen.findByText('Planificación aprobada')).toBeInTheDocument()

    const checkbox = screen.getByLabelText('Solo no leídas')
    fireEvent.click(checkbox)

    expect(screen.getByText('Planificación aprobada')).toBeInTheDocument()
    expect(screen.queryByText('Sesión finalizada')).not.toBeInTheDocument()

    const marcarTodasBtn = screen.getByRole('button', { name: 'Marcar todas como leídas' })
    fireEvent.click(marcarTodasBtn)
    expect(api.marcarTodasNotificacionesLeidas).toHaveBeenCalled()
  })

  it('muestra estado vacío cuando no existen notificaciones', async () => {
    vi.mocked(api.listarNotificaciones).mockResolvedValue([])
    render(<NotificationsPage />)
    expect(await screen.findByText('No hay notificaciones.')).toBeInTheDocument()
  })

  it('muestra mensaje de error si falla la carga', async () => {
    vi.mocked(api.listarNotificaciones).mockRejectedValue(new Error('Fallo al listar'))
    render(<NotificationsPage />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Fallo al listar')
  })
})
