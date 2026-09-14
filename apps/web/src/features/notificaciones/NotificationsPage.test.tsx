import type { ReactNode } from 'react'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
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

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

describe('NotificationsPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockNavigate.mockReset()
    vi.mocked(api.listarNotificaciones).mockResolvedValue([
      {
        id: 'n1',
        titulo: 'Planificación aprobada',
        cuerpo: 'Horario disponible',
        tipo: 'PLANIFICACION',
        referenciaId: null,
        leida: false,
        creadaEn: '2026-09-05T10:00:00Z',
      },
      {
        id: 'n2',
        titulo: 'Sesión finalizada',
        cuerpo: 'Clase registrada',
        tipo: 'ASISTENCIA',
        referenciaId: null,
        leida: true,
        creadaEn: '2026-09-05T09:00:00Z',
      },
    ])
    vi.mocked(api.marcarNotificacionLeida).mockResolvedValue({
      id: 'n1',
      titulo: 'Planificación aprobada',
      cuerpo: 'Horario disponible',
      tipo: 'PLANIFICACION',
      referenciaId: null,
      leida: true,
      creadaEn: '2026-09-05T10:00:00Z',
    })
    vi.mocked(api.marcarTodasNotificacionesLeidas).mockResolvedValue(undefined)
  })

  it('8. muestra historial diferenciado de leídas y no leídas', async () => {
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Planificación aprobada')).toBeInTheDocument()
    expect(screen.getByText('Sesión finalizada')).toBeInTheDocument()

    // Diferenciación visual de estados
    expect(screen.getByText('No leída')).toBeInTheDocument()
    expect(screen.getByText('Leída')).toBeInTheDocument()
  })

  it('permite marcar una notificación individual como leída', async () => {
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>,
    )
    expect(await screen.findByText('Planificación aprobada')).toBeInTheDocument()

    const botonMarcar = screen.getByRole('button', { name: 'Marcar como leída' })
    fireEvent.click(botonMarcar)

    await waitFor(() => {
      expect(api.marcarNotificacionLeida).toHaveBeenCalledWith('n1')
    })
  })

  it('9 y 10. marcar todas como leídas persiste en backend y NO borra el historial', async () => {
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>,
    )
    expect(await screen.findByText('Planificación aprobada')).toBeInTheDocument()

    const marcarTodasBtn = screen.getByRole('button', { name: 'Marcar todas como leídas' })
    fireEvent.click(marcarTodasBtn)

    await waitFor(() => {
      expect(api.marcarTodasNotificacionesLeidas).toHaveBeenCalled()
    })

    // 10. No borra el historial: ambas notificaciones siguen presentes
    expect(screen.getByText('Planificación aprobada')).toBeInTheDocument()
    expect(screen.getByText('Sesión finalizada')).toBeInTheDocument()
  })

  it('permite filtrar solo no leídas', async () => {
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>,
    )
    expect(await screen.findByText('Planificación aprobada')).toBeInTheDocument()

    const checkbox = screen.getByLabelText('Solo no leídas')
    fireEvent.click(checkbox)

    expect(screen.getByText('Planificación aprobada')).toBeInTheDocument()
    expect(screen.queryByText('Sesión finalizada')).not.toBeInTheDocument()
  })

  it('1. muestra estado vacío cuando no existen notificaciones', async () => {
    vi.mocked(api.listarNotificaciones).mockResolvedValue([])
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>,
    )
    expect(await screen.findByText('No hay notificaciones.')).toBeInTheDocument()
  })

  it('muestra mensaje de error si falla la carga', async () => {
    vi.mocked(api.listarNotificaciones).mockRejectedValue(new Error('Fallo al listar'))
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>,
    )
    expect(await screen.findByRole('alert')).toHaveTextContent('Fallo al listar')
  })

  it('11. botón "Ver recurso" navega al destino contextual de la notificación', async () => {
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>,
    )
    expect(await screen.findByText('Planificación aprobada')).toBeInTheDocument()

    const verRecursoBtns = screen.getAllByRole('button', { name: 'Ver recurso' })
    fireEvent.click(verRecursoBtns[0])

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/planificacion')
    })
  })
})
