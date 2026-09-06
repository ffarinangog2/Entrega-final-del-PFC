import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../auth'
import type { AuthContextValue } from '../../auth/context'
import * as api from '../../services/operationalApi'
import * as academico from '../../services/academicoApi'
import { StudentNotificationBell } from './StudentNotificationBell'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

vi.mock('../../services/operationalApi')
vi.mock('../../services/academicoApi')

describe('campana del estudiante', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockNavigate.mockReset()
    vi.mocked(api.obtenerMiHorario).mockResolvedValue([])
    vi.mocked(academico.obtenerMaterias).mockResolvedValue([])
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([])
    vi.mocked(api.listarNotificaciones).mockResolvedValue([])
    vi.mocked(api.listarSesionesAbiertas).mockResolvedValue([])
    vi.mocked(api.marcarNotificacionLeida).mockImplementation(async (id) => ({
      id,
      titulo: 'Leída',
      cuerpo: 'Leída',
      tipo: 'INFO',
      referenciaId: null,
      leida: true,
      creadaEn: '2026-09-06T10:00:00Z',
    }))
    vi.mocked(api.marcarTodasNotificacionesLeidas).mockResolvedValue(undefined)
  })

  it('muestra contador y acceso contextual a asistencia', async () => {
    vi.mocked(api.listarSesionesAbiertas).mockResolvedValue([
      {
        id: 's',
        reservaId: null,
        bloqueId: 'b',
        fechaClase: '2026-09-03',
        abiertaEn: '2026-09-03T14:00:00Z',
        expiraEn: '2026-09-03T14:15:00Z',
        estado: 'ABIERTA',
        token: null,
      },
    ])
    vi.mocked(api.obtenerMiHorario).mockResolvedValue([
      {
        id: 'b',
        planificacionId: 'p',
        nivel: 7,
        periodoId: 'pe',
        carreraId: 'c',
        materiaId: 'm',
        docenteId: 'd',
        laboratorioId: 'l',
        diaSemana: 'LUNES',
        horaInicio: '14:00',
        horaFin: '16:00',
        estado: 'CONFIRMADA',
        observacion: null,
        version: 0,
      },
    ])
    vi.mocked(academico.obtenerMaterias).mockResolvedValue([
      {
        id: 'm',
        carreraId: 'c',
        codigo: 'MAT',
        nombre: 'Aplicaciones',
        numeroHoras: 2,
        nivel: 7,
        activo: true,
      },
    ])
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([
      {
        id: 'l',
        pisoId: 'pi',
        codigo: 'LAB-03',
        nombre: 'Lab 3',
        capacidad: 30,
        descripcion: '',
        estado: 'DISPONIBLE',
        activo: true,
        creadoEn: '',
        actualizadoEn: '',
      },
    ])
    render(
      <MemoryRouter>
        <StudentNotificationBell />
      </MemoryRouter>,
    )
    const button = await screen.findByRole('button', { name: 'Notificaciones: 1 pendientes' })
    fireEvent.click(button)
    expect(screen.getByRole('link', { name: /Aplicaciones · LAB-03/ })).toHaveAttribute(
      'href',
      '/asistencia',
    )
  })

  it('muestra estado vacío sin fabricar notificaciones', async () => {
    vi.mocked(api.listarSesionesAbiertas).mockResolvedValue([])
    render(
      <MemoryRouter>
        <StudentNotificationBell />
      </MemoryRouter>,
    )
    const button = await screen.findByRole('button', { name: 'Notificaciones: 0 pendientes' })
    fireEvent.click(button)
    expect(screen.getByText('No hay notificaciones.')).toBeInTheDocument()
  })

  it('actualiza el contador al recuperar foco', async () => {
    vi.mocked(api.listarSesionesAbiertas).mockResolvedValue([])
    vi.mocked(api.listarNotificaciones)
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([
        {
          id: 'n',
          titulo: 'Planificación',
          cuerpo: 'Pendiente',
          tipo: 'PLANIFICACION',
          referenciaId: 'p',
          leida: false,
          creadaEn: new Date().toISOString(),
        },
      ])
    render(
      <MemoryRouter>
        <StudentNotificationBell asistencia={false} />
      </MemoryRouter>,
    )
    await screen.findByRole('button', { name: 'Notificaciones: 0 pendientes' })
    fireEvent(window, new Event('focus'))
    expect(await screen.findByRole('button', { name: 'Notificaciones: 1 pendientes' })).toBeInTheDocument()
  })

  it('no consulta datos ni registra listeners cuando no está autenticado', async () => {
    const authValue: AuthContextValue = {
      usuario: null,
      isAuthenticated: false,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      refreshSession: vi.fn().mockResolvedValue(false),
    }
    render(
      <AuthContext.Provider value={authValue}>
        <MemoryRouter>
          <StudentNotificationBell />
        </MemoryRouter>
      </AuthContext.Provider>,
    )
    expect(screen.getByRole('button', { name: 'Notificaciones: 0 pendientes' })).toBeInTheDocument()
    expect(api.listarNotificaciones).not.toHaveBeenCalled()
  })

  it('despliega notificaciones, navega y marca como leídas según corresponda', async () => {
    vi.mocked(api.listarNotificaciones).mockResolvedValue([
      {
        id: 'n-unread',
        titulo: 'Aviso retiro',
        cuerpo: 'Solicitud aprobada',
        tipo: 'SOLICITUD_RETIRO',
        referenciaId: 'ret-10',
        leida: false,
        creadaEn: '2026-09-06T12:00:00Z',
      },
      {
        id: 'n-read',
        titulo: 'Uso iniciado',
        cuerpo: 'Laboratorio abierto',
        tipo: 'USO_LABORATORIO_ABIERTO',
        referenciaId: 'ses-20',
        leida: true,
        creadaEn: '2026-09-06T11:00:00Z',
      },
      {
        id: 'n-other',
        titulo: 'Informativo',
        cuerpo: 'Otro aviso',
        tipo: 'GENERAL',
        referenciaId: null,
        leida: false,
        creadaEn: '2026-09-06T10:00:00Z',
      },
    ])

    render(
      <MemoryRouter>
        <StudentNotificationBell />
      </MemoryRouter>,
    )

    const bellBtn = await screen.findByRole('button', { name: 'Notificaciones: 2 pendientes' })
    fireEvent.click(bellBtn)

    // Verifica botón de marcar todas y conteo
    expect(screen.getByText('2 sin leer')).toBeInTheDocument()
    const marcarTodasBtn = screen.getByRole('button', { name: 'Marcar todas como leídas' })
    expect(marcarTodasBtn).not.toBeDisabled()

    // Click en la notificación no leída
    fireEvent.click(screen.getByText('Aviso retiro'))
    await waitFor(() => {
      expect(api.marcarNotificacionLeida).toHaveBeenCalledWith('n-unread')
      expect(mockNavigate).toHaveBeenCalledWith('/planificacion?planificacionId=ret-10')
    })

    // Volver a abrir
    fireEvent.click(bellBtn)

    // Click en notificación ya leída
    vi.mocked(api.marcarNotificacionLeida).mockClear()
    mockNavigate.mockClear()
    fireEvent.click(screen.getByText('Uso iniciado'))
    await waitFor(() => {
      expect(api.marcarNotificacionLeida).not.toHaveBeenCalled()
      expect(mockNavigate).toHaveBeenCalledWith('/asistencia?sesionId=ses-20')
    })

    // Volver a abrir y marcar todas
    fireEvent.click(bellBtn)
    fireEvent.click(screen.getByRole('button', { name: 'Marcar todas como leídas' }))
    await waitFor(() => {
      expect(api.marcarTodasNotificacionesLeidas).toHaveBeenCalled()
    })

    // Click en "Ver todas las notificaciones" cierra el panel
    const verTodasLink = screen.getByRole('link', { name: 'Ver todas las notificaciones' })
    fireEvent.click(verTodasLink)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('usa textos por defecto cuando una sesión abierta no tiene bloque en el horario local', async () => {
    vi.mocked(api.listarSesionesAbiertas).mockResolvedValue([
      {
        id: 'ses-huerfana',
        reservaId: null,
        bloqueId: 'bloque-inexistente',
        fechaClase: '2026-09-06',
        abiertaEn: '2026-09-06T14:00:00Z',
        expiraEn: '2026-09-06T14:15:00Z',
        estado: 'ABIERTA',
        token: null,
      },
    ])
    vi.mocked(api.obtenerMiHorario).mockResolvedValue([])

    render(
      <MemoryRouter>
        <StudentNotificationBell />
      </MemoryRouter>,
    )

    const bellBtn = await screen.findByRole('button', { name: 'Notificaciones: 1 pendientes' })
    fireEvent.click(bellBtn)

    expect(
      screen.getByRole('link', { name: /Asistencia disponible · Actividad de laboratorio · Laboratorio/ }),
    ).toHaveAttribute('href', '/asistencia')
  })
})
