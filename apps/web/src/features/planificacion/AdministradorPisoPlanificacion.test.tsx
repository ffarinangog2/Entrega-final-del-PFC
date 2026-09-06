import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as academico from '../../services/academicoApi'
import * as operational from '../../services/operationalApi'
import { AdministradorPisoPlanificacion } from './AdministradorPisoPlanificacion'
import { estadoPaquete } from './adminPisoPlanificacionState'

vi.mock('../../services/academicoApi')
vi.mock('../../services/operationalApi')
vi.mock('../../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => (
    <>{children}</>
  ),
}))

const plan = (
  id: string,
  estado: operational.EstadoPlanificacion = 'ENVIADA',
): operational.Planificacion => ({
  id,
  planificacionId: 'aggregate-1',
  periodoId: 'periodo-uuid',
  carreraId: 'carrera-uuid',
  materiaId: 'materia-uuid',
  docenteId: 'docente-uuid',
  laboratorioId: 'laboratorio-uuid',
  diaSemana: 'LUNES',
  horaInicio: id === 'plan-1' ? '07:30' : '09:30',
  horaFin: id === 'plan-1' ? '09:30' : '11:30',
  estado,
  observacion: null,
  version: 0,
})

describe('AdministradorPisoPlanificacion', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(operational.listarSolicitudesRetiro).mockResolvedValue([])
    vi.mocked(operational.listarSolicitudesRetiro).mockResolvedValue([])
    vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([])
    vi.mocked(operational.aprobarSolicitudCambio).mockResolvedValue({} as operational.SolicitudCambio)
    vi.mocked(operational.rechazarSolicitudCambio).mockResolvedValue({} as operational.SolicitudCambio)
    vi.mocked(academico.obtenerPisos).mockResolvedValue([
      { id: 'piso-uuid', bloqueId: 'bloque-1', numero: 1, descripcion: 'Primer Piso', activo: true },
      { id: 'piso-destino-uuid', bloqueId: 'bloque-1', numero: 2, descripcion: 'Segundo Piso', activo: true },
    ])
    vi.mocked(academico.obtenerDocentesPlanificacion).mockResolvedValue([
      { id: 'docente-uuid', perfilId: 'perfil-1', codigoDocente: 'DOC-CARLOS', activo: true },
      { id: 'docente-dest-uuid', perfilId: 'perfil-2', codigoDocente: 'DOC-ANA', activo: true },
    ])
    vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([{
      id: 'aggregate-1', carreraId: 'carrera-uuid', periodoId: 'periodo-uuid',
      estado: 'EN_REVISION', bloques: [plan('plan-1'), plan('plan-2')],
      revisiones: [],
    }])
    vi.mocked(academico.obtenerMaterias).mockResolvedValue([
      {
        id: 'materia-uuid',
        carreraId: 'carrera-uuid',
        codigo: 'PROG',
        nombre: 'Programación Web',
        numeroHoras: 4,
        activo: true,
      },
    ])
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([
      {
        id: 'laboratorio-uuid',
        pisoId: 'piso-uuid',
        codigo: 'LAB-01',
        nombre: 'Laboratorio de Software',
        capacidad: 30,
        descripcion: '',
        estado: 'DISPONIBLE',
        activo: true,
        creadoEn: '',
        actualizadoEn: '',
      },
      {
        id: 'laboratorio-destino-uuid',
        pisoId: 'piso-destino-uuid',
        codigo: 'LAB-02',
        nombre: 'Laboratorio de Redes',
        capacidad: 25,
        descripcion: '',
        estado: 'DISPONIBLE',
        activo: true,
        creadoEn: '',
        actualizadoEn: '',
      },
    ])
    vi.mocked(academico.obtenerCarreras).mockResolvedValue([
      {
        id: 'carrera-uuid',
        facultadId: 'facultad',
        codigo: 'IS',
        nombre: 'Ingeniería de Software',
        activo: true,
      },
    ])
    vi.mocked(academico.obtenerPeriodoActual).mockResolvedValue({
      id: 'periodo-uuid',
      codigo: '2026-B',
      nombre: 'Periodo 2026-B',
      fechaInicio: '',
      fechaFin: '',
      estado: 'ACTIVO',
    })
  })

  it('presenta la planificación completa con nombres humanos y sin UUID visibles', async () => {
    render(<AdministradorPisoPlanificacion />)
    expect(
      await screen.findByText('Ingeniería de Software'),
    ).toBeInTheDocument()
    expect(screen.getAllByText('Programación Web')).toHaveLength(2)
    expect(screen.getAllByText('LAB-01')).toHaveLength(2)
    expect(screen.queryByText('materia-uuid')).not.toBeInTheDocument()
    expect(screen.getByText('2 bloques en su piso')).toBeInTheDocument()
  })

  it('aprueba todos los bloques pendientes mediante una sola confirmación', async () => {
    render(<AdministradorPisoPlanificacion />)
    fireEvent.click(
      await screen.findByRole('button', { name: 'Aprobar planificación' }),
    )
    await waitFor(() =>
      expect(operational.aprobarPlanificacionPiso).toHaveBeenCalledWith(
        'aggregate-1',
      ),
    )
    expect(window.confirm).toHaveBeenCalledTimes(1)
  })

  it('exige motivo y rechaza el conjunto', async () => {
    render(<AdministradorPisoPlanificacion />)
    const button = await screen.findByRole('button', {
      name: 'Rechazar planificación',
    })
    expect(button).toBeDisabled()
    fireEvent.change(screen.getByLabelText('Motivo del rechazo'), {
      target: { value: 'Conflictos en LAB-01' },
    })
    fireEvent.click(button)
    await waitFor(() =>
      expect(operational.rechazarPlanificacionPiso).toHaveBeenCalledWith(
        'aggregate-1',
        'Conflictos en LAB-01',
      ),
    )
  })

  it('marca un bloque y envía una propuesta con observación', async () => {
    render(<AdministradorPisoPlanificacion />)
    fireEvent.click(
      (await screen.findAllByRole('button', { name: 'Marcar cambio' }))[0],
    )
    fireEvent.change(screen.getByLabelText('Observación'), {
      target: { value: 'Laboratorio en mantenimiento' },
    })
    fireEvent.click(
      screen.getByRole('button', { name: 'Enviar observaciones/propuestas' }),
    )
    await waitFor(() =>
      expect(operational.proponerCambioPlanificacionPiso).toHaveBeenCalledWith(
        'aggregate-1',
        expect.objectContaining({
          bloqueId: 'plan-1',
          observacion: 'Laboratorio en mantenimiento',
        }),
      ),
    )
  })

  it('muestra error controlado cuando falla el API', async () => {
    vi.mocked(operational.listarPlanificacionesAgregadas).mockRejectedValue(
      new Error('No tiene un piso institucional asignado.'),
    )
    render(<AdministradorPisoPlanificacion />)
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No tiene un piso institucional asignado.',
    )
    expect(
      screen.queryByText(
        'No existe una planificación enviada para los laboratorios de su piso.',
      ),
    ).not.toBeInTheDocument()
  })

  it('mantiene etiquetas coherentes de estado del paquete', () => {
    expect(estadoPaquete([plan('p', 'ENVIADA')])).toBe('Pendiente de revisión')
    expect(estadoPaquete([plan('p', 'CONFIRMADA')])).toBe('Aprobada')
    expect(estadoPaquete([plan('p', 'PROPUESTA_CAMBIO')])).toBe(
      'Devuelta con observaciones',
    )
  })

  const solicitudBase: operational.SolicitudCambio = {
    id: 'sol-1',
    planificacionId: 'aggregate-1',
    bloqueId: 'plan-1',
    tipo: 'LABORATORIO',
    estado: 'PENDIENTE',
    motivo: 'Mantenimiento del laboratorio original',
    solicitantePerfilId: 'perfil-coord',
    laboratorioAnteriorId: 'laboratorio-uuid',
    laboratorioPropuestoId: 'laboratorio-destino-uuid',
    docenteAnteriorId: 'docente-uuid',
    docentePropuestoId: 'docente-uuid',
    diaAnterior: 'LUNES',
    diaPropuesto: 'LUNES',
    horaInicioAnterior: '07:30',
    horaInicioPropuesta: '07:30',
    horaFinAnterior: '09:30',
    horaFinPropuesta: '09:30',
    creadaEn: '2026-09-06T10:00:00Z',
    revisiones: [
      { pisoId: 'piso-uuid', estado: 'PENDIENTE', revisorPerfilId: null, observacion: null, resueltaEn: null },
      { pisoId: 'piso-destino-uuid', estado: 'PENDIENTE', revisorPerfilId: null, observacion: null, resueltaEn: null },
    ],
  }

  it('renderiza tarjeta enriquecida sin UUID visibles y con transición de nombres de piso', async () => {
    vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([solicitudBase])
    render(<AdministradorPisoPlanificacion />)

    expect(await screen.findByText('Solicitud de cambio · LABORATORIO')).toBeInTheDocument()
    expect(screen.getByText('Solicitante: Coordinación académica')).toBeInTheDocument()
    expect(screen.getByText(/Bloque afectado: Programación Web \(Lunes 07:30–09:30\)/)).toBeInTheDocument()
    expect(screen.getByText(/Laboratorio: LAB-01 \(Primer Piso\) → LAB-02 \(Segundo Piso\)/)).toBeInTheDocument()
    expect(screen.getByText(/Piso: Primer Piso → Segundo Piso/)).toBeInTheDocument()
    expect(screen.getByText('Motivo: Mantenimiento del laboratorio original')).toBeInTheDocument()
    expect(screen.getByText(/El horario original continúa vigente mientras la solicitud esté pendiente/)).toBeInTheDocument()

    expect(screen.queryByText('laboratorio-uuid')).not.toBeInTheDocument()
    expect(screen.queryByText('piso-uuid')).not.toBeInTheDocument()
    expect(screen.queryByText('perfil-coord')).not.toBeInTheDocument()
  })

  it('muestra botones de aprobar/rechazar solo si la solicitud está pendiente para este piso', async () => {
    vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([solicitudBase])
    render(<AdministradorPisoPlanificacion />)

    expect(await screen.findByRole('button', { name: 'Aprobar cambio' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Rechazar cambio' })).toBeInTheDocument()
  })

  it("muestra 'Cambio aprobado por este piso' y oculta botones cuando este piso ya aprobó", async () => {
    vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([{
      ...solicitudBase,
      revisiones: [
        { pisoId: 'piso-uuid', estado: 'APROBADA', revisorPerfilId: 'rev-1', observacion: 'Aprobado', resueltaEn: '2026-09-06T11:00:00Z' },
        { pisoId: 'piso-destino-uuid', estado: 'PENDIENTE', revisorPerfilId: null, observacion: null, resueltaEn: null },
      ],
    }])
    render(<AdministradorPisoPlanificacion />)

    expect(await screen.findByText('Cambio aprobado por este piso')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Aprobar cambio' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Rechazar cambio' })).not.toBeInTheDocument()
  })

  it("muestra 'Cambio rechazado por este piso' y oculta botones cuando este piso ya rechazó", async () => {
    vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([{
      ...solicitudBase,
      revisiones: [
        { pisoId: 'piso-uuid', estado: 'RECHAZADA', revisorPerfilId: 'rev-1', observacion: 'Rechazado', resueltaEn: '2026-09-06T11:00:00Z' },
      ],
    }])
    render(<AdministradorPisoPlanificacion />)

    expect(await screen.findByText('Cambio rechazado por este piso')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Aprobar cambio' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Rechazar cambio' })).not.toBeInTheDocument()
  })

  it('al pulsar aprobar llama a aprobarSolicitudCambio y refresca para ocultar botones', async () => {
    vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([solicitudBase])
    vi.mocked(operational.aprobarSolicitudCambio).mockImplementation(async () => {
      vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([{
        ...solicitudBase,
        revisiones: [
          { pisoId: 'piso-uuid', estado: 'APROBADA', revisorPerfilId: 'rev-1', observacion: 'Ok', resueltaEn: '2026-09-06T12:00:00Z' },
        ],
      }])
      return {} as operational.SolicitudCambio
    })

    render(<AdministradorPisoPlanificacion />)
    const botonAprobar = await screen.findByRole('button', { name: 'Aprobar cambio' })
    fireEvent.click(botonAprobar)

    await waitFor(() => {
      expect(operational.aprobarSolicitudCambio).toHaveBeenCalledWith('aggregate-1', 'sol-1')
    })
    expect(await screen.findByText('Cambio aprobado por este piso')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Aprobar cambio' })).not.toBeInTheDocument()
  })

  it('al pulsar rechazar solicita motivo con prompt, llama a rechazarSolicitudCambio y refresca', async () => {
    vi.spyOn(window, 'prompt').mockReturnValue('Sin disponibilidad en el piso')
    vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([solicitudBase])
    vi.mocked(operational.rechazarSolicitudCambio).mockImplementation(async () => {
      vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([{
        ...solicitudBase,
        estado: 'RECHAZADA',
        revisiones: [
          { pisoId: 'piso-uuid', estado: 'RECHAZADA', revisorPerfilId: 'rev-1', observacion: 'Sin disponibilidad en el piso', resueltaEn: '2026-09-06T12:00:00Z' },
        ],
      }])
      return {} as operational.SolicitudCambio
    })

    render(<AdministradorPisoPlanificacion />)
    const botonRechazar = await screen.findByRole('button', { name: 'Rechazar cambio' })
    fireEvent.click(botonRechazar)

    expect(window.prompt).toHaveBeenCalledWith('Motivo del rechazo')
    await waitFor(() => {
      expect(operational.rechazarSolicitudCambio).toHaveBeenCalledWith('aggregate-1', 'sol-1', 'Sin disponibilidad en el piso')
    })
    expect(await screen.findByText('Cambio rechazado por este piso')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Rechazar cambio' })).not.toBeInTheDocument()
  })
})
