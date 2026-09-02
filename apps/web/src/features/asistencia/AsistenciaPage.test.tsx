import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '../../services/operationalApi'
import * as academico from '../../services/academicoApi'
import * as reservas from '../reservas/reservasApi'
import { AsistenciaPage } from './AsistenciaPage'

vi.mock('../../services/operationalApi')
vi.mock('../../services/academicoApi')
vi.mock('../reservas/reservasApi')
let rol = 'ESTUDIANTE'
vi.mock('../../auth', () => ({
  hasRole: (_user: unknown, esperado: string) => esperado === rol,
  useAuth: () => ({
    usuario: { perfilId: 'perfil-autenticado', roles: [rol] },
  }),
}))
vi.mock('../../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => (
    <>{children}</>
  ),
}))

const sesion: api.SesionAsistencia = {
  id: 'sesion-uuid',
  reservaId: 'reserva-uuid',
  abiertaEn: '2026-09-01T15:00:00Z',
  expiraEn: '2026-09-01T15:15:00Z',
  estado: 'ABIERTA',
  token: null,
}
const registro: api.RegistroAsistencia = {
  id: 'registro-uuid',
  sesionId: 'sesion-uuid',
  estudianteId: 'estudiante-uuid',
  registradaEn: '2026-09-01T15:07:00Z',
  estado: 'PRESENTE',
}

describe('AsistenciaPage para estudiante', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    rol = 'ESTUDIANTE'
    vi.mocked(api.listarSesionesAbiertas).mockResolvedValue([sesion])
    vi.mocked(api.historialAsistencia).mockResolvedValue([])
    vi.mocked(api.registrarAsistenciaPropia).mockResolvedValue(registro)
  })

  it('muestra registro disponible sin UUID ni token y usa identidad autenticada', async () => {
    render(<AsistenciaPage />)
    expect(await screen.findByText('Registro habilitado')).toBeInTheDocument()
    expect(screen.queryByText('sesion-uuid')).not.toBeInTheDocument()
    expect(screen.queryByText('perfil-autenticado')).not.toBeInTheDocument()
    expect(screen.queryByText(/token/i)).not.toBeInTheDocument()
    fireEvent.click(
      screen.getByRole('button', { name: 'Registrar mi presencia' }),
    )
    await waitFor(() =>
      expect(api.registrarAsistenciaPropia).toHaveBeenCalledWith('sesion-uuid'),
    )
    expect(api.registrarAsistenciaPropia).toHaveBeenCalledTimes(1)
  })

  it('impide doble registro y muestra historial propio', async () => {
    vi.mocked(api.historialAsistencia).mockResolvedValue([registro])
    render(<AsistenciaPage />)
    expect(
      await screen.findByText('Tu presencia ya fue registrada en esta sesión.'),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Registrar mi presencia' }),
    ).not.toBeInTheDocument()
    expect(screen.getByText('PRESENTE')).toBeInTheDocument()
  })

  it('muestra estado vacío comprensible', async () => {
    vi.mocked(api.listarSesionesAbiertas).mockResolvedValue([])
    render(<AsistenciaPage />)
    expect(
      await screen.findByText(
        'No hay registros de laboratorio habilitados en este momento.',
      ),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Aún no tienes registros de uso.'),
    ).toBeInTheDocument()
  })

  it('muestra error API sin dejar la vista en blanco', async () => {
    vi.mocked(api.listarSesionesAbiertas).mockRejectedValue(
      new Error('No fue posible consultar el registro.'),
    )
    render(<AsistenciaPage />)
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No fue posible consultar el registro.',
    )
    expect(
      screen.getByRole('heading', { name: 'Registro de laboratorio' }),
    ).toBeInTheDocument()
  })

  it('docente conserva carga, habilitación, actualización y cierre de sesión', async () => {
    rol = 'DOCENTE'
    const hoy = new Date().toISOString().slice(0, 10)
    vi.mocked(reservas.obtenerReservas).mockResolvedValue([
      {
        id: 'reserva-1',
        solicitudId: 'solicitud-1',
        laboratorioId: 'lab-1',
        responsableId: 'perfil-autenticado',
        fechaReserva: hoy,
        horaInicio: '10:30',
        horaFin: '12:30',
        estado: 'PROGRAMADA',
        codigoReserva: 'RES-001',
        creadaEn: '',
        actualizadaEn: '',
        version: 0,
      },
    ])
    vi.mocked(reservas.obtenerSolicitudPorId).mockResolvedValue({
      id: 'solicitud-1',
      solicitanteId: 'perfil-autenticado',
      docenteId: 'docente-1',
      laboratorioId: 'lab-1',
      materiaId: 'materia-1',
      periodoLectivoId: 'periodo-1',
      fechaReserva: hoy,
      horaInicio: '10:30',
      horaFin: '12:30',
      numeroParticipantes: 20,
      motivo: 'Clase',
      estado: 'APROBADA',
      observacion: '',
      propuestaLaboratorioId: null,
      propuestaFecha: null,
      propuestaHoraInicio: null,
      propuestaHoraFin: null,
      propuestaObservacion: null,
      reservaId: 'reserva-1',
      creadaEn: '',
      actualizadaEn: '',
      version: 0,
    })
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([
      {
        id: 'lab-1',
        pisoId: 'piso-1',
        codigo: 'LAB-01',
        nombre: 'Laboratorio de Software',
        capacidad: 30,
        descripcion: '',
        estado: 'DISPONIBLE',
        activo: true,
        creadoEn: '',
        actualizadoEn: '',
      },
    ])
    vi.mocked(academico.obtenerMaterias).mockResolvedValue([
      {
        id: 'materia-1',
        carreraId: 'carrera-1',
        codigo: 'BDD',
        nombre: 'Bases de Datos',
        numeroHoras: 4,
        activo: true,
      },
    ])
    vi.mocked(api.abrirAsistencia).mockResolvedValue(sesion)
    vi.mocked(api.listarAsistentes).mockResolvedValue([registro])
    vi.mocked(api.consultarAsistencia).mockResolvedValue(sesion)
    vi.mocked(api.cerrarAsistencia).mockResolvedValue(undefined)
    vi.spyOn(window, 'confirm').mockReturnValue(true)

    render(<AsistenciaPage />)
    fireEvent.click(
      await screen.findByRole('button', { name: 'Habilitar asistencia' }),
    )
    expect(await screen.findByText('Asistencia habilitada')).toBeInTheDocument()
    fireEvent.click(
      screen.getByRole('button', { name: 'Actualizar asistentes' }),
    )
    await waitFor(() =>
      expect(api.listarAsistentes).toHaveBeenCalledWith('sesion-uuid'),
    )
    fireEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }))
    await waitFor(() =>
      expect(api.cerrarAsistencia).toHaveBeenCalledWith('sesion-uuid'),
    )
  })
})
