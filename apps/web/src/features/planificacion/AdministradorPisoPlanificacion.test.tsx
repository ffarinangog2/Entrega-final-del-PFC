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
    vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([])
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

  it('muestra paquete y solicitudes a administrador de piso destino aunque tenga 0 bloques', async () => {
    vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([
      {
        id: 'aggregate-dest',
        carreraId: 'carrera-uuid',
        periodoId: 'periodo-uuid',
        estado: 'APROBADA',
        bloques: [],
        revisiones: [],
        pisoGestionadoId: 'piso-destino-uuid',
      },
    ])
    vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([
      {
        id: 'sol-1',
        planificacionId: 'aggregate-dest',
        bloqueId: 'bloque-1',
        tipo: 'LABORATORIO',
        estado: 'PENDIENTE',
        motivo: 'Cambio hacia piso destino',
        laboratorioAnteriorId: 'lab-origen',
        laboratorioPropuestoId: 'laboratorio-uuid',
        docenteAnteriorId: null,
        docentePropuestoId: null,
        diaAnterior: 'LUNES',
        diaPropuesto: 'LUNES',
        horaInicioAnterior: '08:00',
        horaInicioPropuesta: '08:00',
        horaFinAnterior: '10:00',
        horaFinPropuesta: '10:00',
        creadaEn: new Date().toISOString(),
        revisiones: [
          { pisoId: 'piso-destino-uuid', estado: 'PENDIENTE' },
        ],
      },
    ])

    render(<AdministradorPisoPlanificacion />)
    expect(await screen.findByText('0 bloques en su piso')).toBeInTheDocument()
    expect(screen.getByText('Solicitud de cambio · LABORATORIO')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Aprobar cambio' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Rechazar cambio' })).toBeInTheDocument()
  })

  it('no muestra en la lista de solicitudes pendientes si la revision de este piso ya fue APROBADA', async () => {
    vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([
      {
        id: 'aggregate-dest',
        carreraId: 'carrera-uuid',
        periodoId: 'periodo-uuid',
        estado: 'APROBADA',
        bloques: [],
        revisiones: [],
        pisoGestionadoId: 'piso-destino-uuid',
      },
    ])
    vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([
      {
        id: 'sol-1',
        planificacionId: 'aggregate-dest',
        bloqueId: 'bloque-1',
        tipo: 'LABORATORIO',
        estado: 'PENDIENTE',
        motivo: 'Cambio hacia piso destino',
        laboratorioAnteriorId: 'lab-origen',
        laboratorioPropuestoId: 'laboratorio-uuid',
        docenteAnteriorId: null,
        docentePropuestoId: null,
        diaAnterior: 'LUNES',
        diaPropuesto: 'LUNES',
        horaInicioAnterior: '08:00',
        horaInicioPropuesta: '08:00',
        horaFinAnterior: '10:00',
        horaFinPropuesta: '10:00',
        creadaEn: new Date().toISOString(),
        revisiones: [
          { pisoId: 'piso-destino-uuid', estado: 'APROBADA' },
        ],
      },
    ])

    render(<AdministradorPisoPlanificacion />)
    await screen.findByText('0 bloques en su piso')
    expect(screen.queryByText('Solicitud de cambio · LABORATORIO')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Aprobar cambio' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Rechazar cambio' })).not.toBeInTheDocument()
  })

  it('no muestra en la lista de solicitudes pendientes si la revision de este piso ya fue RECHAZADA', async () => {
    vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([
      {
        id: 'aggregate-dest',
        carreraId: 'carrera-uuid',
        periodoId: 'periodo-uuid',
        estado: 'APROBADA',
        bloques: [],
        revisiones: [],
        pisoGestionadoId: 'piso-destino-uuid',
      },
    ])
    vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([
      {
        id: 'sol-1',
        planificacionId: 'aggregate-dest',
        bloqueId: 'bloque-1',
        tipo: 'LABORATORIO',
        estado: 'PENDIENTE',
        motivo: 'Cambio hacia piso destino',
        laboratorioAnteriorId: 'lab-origen',
        laboratorioPropuestoId: 'laboratorio-uuid',
        docenteAnteriorId: null,
        docentePropuestoId: null,
        diaAnterior: 'LUNES',
        diaPropuesto: 'LUNES',
        horaInicioAnterior: '08:00',
        horaInicioPropuesta: '08:00',
        horaFinAnterior: '10:00',
        horaFinPropuesta: '10:00',
        creadaEn: new Date().toISOString(),
        revisiones: [
          { pisoId: 'piso-destino-uuid', estado: 'RECHAZADA' },
        ],
      },
    ])

    render(<AdministradorPisoPlanificacion />)
    await screen.findByText('0 bloques en su piso')
    expect(screen.queryByText('Solicitud de cambio · LABORATORIO')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Aprobar cambio' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Rechazar cambio' })).not.toBeInTheDocument()
  })

  it('no muestra la solicitud si no tiene revision correspondiente a mi piso', async () => {
    vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([
      {
        id: 'aggregate-dest',
        carreraId: 'carrera-uuid',
        periodoId: 'periodo-uuid',
        estado: 'APROBADA',
        bloques: [],
        revisiones: [],
        pisoGestionadoId: 'piso-destino-uuid',
      },
    ])
    vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([
      {
        id: 'sol-1',
        planificacionId: 'aggregate-dest',
        bloqueId: 'bloque-1',
        tipo: 'LABORATORIO',
        estado: 'PENDIENTE',
        motivo: 'Cambio hacia otro piso',
        laboratorioAnteriorId: 'lab-origen',
        laboratorioPropuestoId: 'lab-otro',
        docenteAnteriorId: null,
        docentePropuestoId: null,
        diaAnterior: 'LUNES',
        diaPropuesto: 'LUNES',
        horaInicioAnterior: '08:00',
        horaInicioPropuesta: '08:00',
        horaFinAnterior: '10:00',
        horaFinPropuesta: '10:00',
        creadaEn: new Date().toISOString(),
        revisiones: [
          { pisoId: 'otro-piso-uuid', estado: 'PENDIENTE' },
        ],
      },
    ])

    render(<AdministradorPisoPlanificacion />)
    await screen.findByText('0 bloques en su piso')
    expect(screen.queryByText('Solicitud de cambio · LABORATORIO')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Aprobar cambio' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Rechazar cambio' })).not.toBeInTheDocument()
  })
})
