import type { ReactNode } from 'react'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { NotificationsPage } from './NotificationsPage'
import * as api from '../../services/operationalApi'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

vi.mock('../../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: ReactNode }) => <>{children}</>,
}))

vi.mock('../../services/operationalApi', () => ({
  listarNotificaciones: vi.fn(),
  listarNotificacionesPaginadas: vi.fn(),
  marcarNotificacionLeida: vi.fn(),
  marcarTodasNotificacionesLeidas: vi.fn(),
}))

describe('NotificationsPage', () => {
  beforeEach(() => {
    mockNavigate.mockReset()
    vi.mocked(api.listarNotificaciones).mockResolvedValue([
      {
        id: 'n1',
        titulo: 'Planificación aprobada',
        cuerpo: 'Horario disponible',
        tipo: 'PLANIFICACION',
        referenciaId: 'plan-1',
        leida: false,
        creadaEn: '2026-09-05T10:00:00Z',
      },
      {
        id: 'n2',
        titulo: 'Sesión finalizada',
        cuerpo: 'Clase registrada',
        tipo: 'ASISTENCIA_ABIERTA',
        referenciaId: null,
        leida: true,
        creadaEn: '2026-09-05T09:00:00Z',
      },
    ])
    vi.mocked(api.listarNotificacionesPaginadas).mockResolvedValue(undefined as never)
    vi.mocked(api.marcarNotificacionLeida).mockResolvedValue({
      id: 'n1',
      titulo: 'Planificación aprobada',
      cuerpo: 'Horario disponible',
      tipo: 'PLANIFICACION',
      referenciaId: 'plan-1',
      leida: true,
      creadaEn: '2026-09-05T10:00:00Z',
    })
    vi.mocked(api.marcarTodasNotificacionesLeidas).mockResolvedValue(undefined)
  })

  it('lista y permite marcar una notificación propia como leída', async () => {
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>,
    )
    expect(await screen.findByText('Planificación aprobada')).toBeInTheDocument()
    expect(screen.getByText('Sesión finalizada')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Marcar como leída' }))
    expect(api.marcarNotificacionLeida).toHaveBeenCalledWith('n1')
  })

  it('permite filtrar solo no leídas y marcar todas como leídas', async () => {
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

    const marcarTodasBtn = screen.getByRole('button', { name: 'Marcar todas como leídas' })
    fireEvent.click(marcarTodasBtn)
    expect(api.marcarTodasNotificacionesLeidas).toHaveBeenCalled()
  })

  it('navega al hacer click en la notificación, marcando como leída solo si estaba sin leer', async () => {
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>,
    )
    expect(await screen.findByText('Planificación aprobada')).toBeInTheDocument()

    // Click en la notificación no leída -> debe marcar y navegar
    fireEvent.click(screen.getByText('Planificación aprobada'))
    await waitFor(() => {
      expect(api.marcarNotificacionLeida).toHaveBeenCalledWith('n1')
      expect(mockNavigate).toHaveBeenCalledWith('/planificacion?planificacionId=plan-1')
    })

    // Click en la notificación ya leída -> no debe llamar marcarNotificacionLeida
    vi.mocked(api.marcarNotificacionLeida).mockClear()
    mockNavigate.mockClear()
    fireEvent.click(screen.getByText('Sesión finalizada'))
    await waitFor(() => {
      expect(api.marcarNotificacionLeida).not.toHaveBeenCalled()
      expect(mockNavigate).toHaveBeenCalledWith('/asistencia')
    })
  })

  it('soporta paginación cuando el servicio paginado está disponible', async () => {
    vi.mocked(api.listarNotificacionesPaginadas).mockResolvedValue({
      content: [
        {
          id: 'n-pag-1',
          titulo: 'Notificación Paginada',
          cuerpo: 'Contenido página 0',
          tipo: 'SOLICITUD_RETIRO',
          referenciaId: 'ret-1',
          leida: false,
          creadaEn: '2026-09-05T10:00:00Z',
        },
      ],
      totalPages: 3,
      totalElements: 3,
      size: 10,
      number: 0,
    })

    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Notificación Paginada')).toBeInTheDocument()
    expect(screen.getByText('Página 1 de 3')).toBeInTheDocument()

    const anteriorBtn = screen.getByRole('button', { name: 'Anterior' })
    const siguienteBtn = screen.getByRole('button', { name: 'Siguiente' })

    expect(anteriorBtn).toBeDisabled()
    expect(siguienteBtn).not.toBeDisabled()

    fireEvent.click(siguienteBtn)
    expect(api.listarNotificacionesPaginadas).toHaveBeenCalledWith(1, 10)
  })

  it('realiza fallback a listarNotificaciones si la paginación no retorna content o falla', async () => {
    vi.mocked(api.listarNotificacionesPaginadas).mockRejectedValue(new Error('Paginación no soportada'))

    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Planificación aprobada')).toBeInTheDocument()
    expect(api.listarNotificaciones).toHaveBeenCalled()
  })

  it('muestra estado vacío cuando no existen notificaciones', async () => {
    vi.mocked(api.listarNotificaciones).mockResolvedValue([])
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>,
    )
    expect(await screen.findByText('No hay notificaciones.')).toBeInTheDocument()
  })

  it('muestra mensaje de error si falla la carga con Error y con error genérico', async () => {
    vi.mocked(api.listarNotificaciones).mockRejectedValue(new Error('Fallo al listar'))
    const { unmount } = render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>,
    )
    expect(await screen.findByRole('alert')).toHaveTextContent('Fallo al listar')
    unmount()

    // Error no estándar (string)
    vi.mocked(api.listarNotificaciones).mockRejectedValue('Error desconocido')
    render(
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>,
    )
    expect(await screen.findByRole('alert')).toHaveTextContent('No fue posible cargar las notificaciones')
  })
})
