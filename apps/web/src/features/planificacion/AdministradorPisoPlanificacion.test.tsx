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
      {
        id: 'lab-origen',
        pisoId: 'piso-origen-uuid',
        codigo: 'LAB-PB-01',
        nombre: 'Laboratorio Planta Baja',
        capacidad: 25,
        descripcion: '',
        estado: 'DISPONIBLE',
        activo: true,
        creadoEn: '',
        actualizadoEn: '',
      },
      {
        id: 'lab-destino',
        pisoId: 'piso-destino-uuid',
        codigo: 'LAB-P1-02',
        nombre: 'Laboratorio Piso 1',
        capacidad: 30,
        descripcion: '',
        estado: 'DISPONIBLE',
        activo: true,
        creadoEn: '',
        actualizadoEn: '',
      },
    ])
    vi.mocked(academico.obtenerPisos).mockResolvedValue([
      { id: 'piso-uuid', bloqueId: 'b1', numero: 1, descripcion: '', activo: true },
      { id: 'piso-origen-uuid', bloqueId: 'b1', numero: 0, descripcion: 'Planta Baja', activo: true },
      { id: 'piso-destino-uuid', bloqueId: 'b1', numero: 1, descripcion: 'Piso 1', activo: true },
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
    expect(screen.getByText('Cambio de laboratorio')).toBeInTheDocument()
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
    expect(screen.queryByRole('button', { name: 'Aprobar cambio' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Rechazar cambio' })).not.toBeInTheDocument()
  })

  describe('ETAPA 5A - Detalles de solicitud y migración de bloques (13 a 24)', () => {
    const solicitudBase: operational.SolicitudCambio = {
      id: 'sol-5a',
      planificacionId: 'aggregate-1',
      bloqueId: 'plan-1',
      tipo: 'LABORATORIO',
      estado: 'PENDIENTE',
      motivo: 'Falta de equipamiento especializado en origen',
      laboratorioAnteriorId: 'lab-origen',
      laboratorioPropuestoId: 'lab-destino',
      materiaId: 'materia-uuid',
      docenteAnteriorId: null,
      docentePropuestoId: null,
      diaAnterior: 'LUNES',
      diaPropuesto: 'LUNES',
      horaInicioAnterior: '07:30',
      horaInicioPropuesta: '07:30',
      horaFinAnterior: '09:30',
      horaFinPropuesta: '09:30',
      creadaEn: new Date().toISOString(),
      revisiones: [
        { pisoId: 'piso-origen-uuid', estado: 'PENDIENTE' },
        { pisoId: 'piso-destino-uuid', estado: 'PENDIENTE' },
      ],
    }

    it('13. muestra laboratorio actual', async () => {
      vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([{
        id: 'aggregate-1', carreraId: 'carrera-uuid', periodoId: 'periodo-uuid',
        estado: 'APROBADA', bloques: [plan('plan-1')], revisiones: [],
        pisoGestionadoId: 'piso-origen-uuid',
      }])
      vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([solicitudBase])

      render(<AdministradorPisoPlanificacion />)
      expect(await screen.findByText('Actual:')).toBeInTheDocument()
      expect(screen.getByText(/LAB-PB-01/)).toBeInTheDocument()
    })

    it('14. muestra laboratorio solicitado', async () => {
      vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([{
        id: 'aggregate-1', carreraId: 'carrera-uuid', periodoId: 'periodo-uuid',
        estado: 'APROBADA', bloques: [plan('plan-1')], revisiones: [],
        pisoGestionadoId: 'piso-origen-uuid',
      }])
      vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([solicitudBase])

      render(<AdministradorPisoPlanificacion />)
      expect(await screen.findByText('Solicitado:')).toBeInTheDocument()
      expect(screen.getByText(/LAB-P1-02/)).toBeInTheDocument()
    })

    it('15. muestra horario', async () => {
      vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([{
        id: 'aggregate-1', carreraId: 'carrera-uuid', periodoId: 'periodo-uuid',
        estado: 'APROBADA', bloques: [plan('plan-1')], revisiones: [],
        pisoGestionadoId: 'piso-origen-uuid',
      }])
      vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([solicitudBase])

      render(<AdministradorPisoPlanificacion />)
      expect(await screen.findByText('Horario:')).toBeInTheDocument()
      expect(screen.getByText(/Lunes · 07:30 - 09:30/)).toBeInTheDocument()
    })

    it('16. muestra motivo', async () => {
      vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([{
        id: 'aggregate-1', carreraId: 'carrera-uuid', periodoId: 'periodo-uuid',
        estado: 'APROBADA', bloques: [plan('plan-1')], revisiones: [],
        pisoGestionadoId: 'piso-origen-uuid',
      }])
      vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([solicitudBase])

      render(<AdministradorPisoPlanificacion />)
      expect(await screen.findByText('Motivo:')).toBeInTheDocument()
      expect(screen.getByText('Falta de equipamiento especializado en origen')).toBeInTheDocument()
    })

    it('17. muestra estados de revisiones por piso', async () => {
      vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([{
        id: 'aggregate-1', carreraId: 'carrera-uuid', periodoId: 'periodo-uuid',
        estado: 'APROBADA', bloques: [plan('plan-1')], revisiones: [],
        pisoGestionadoId: 'piso-origen-uuid',
      }])
      vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([{
        ...solicitudBase,
        revisiones: [
          { pisoId: 'piso-origen-uuid', estado: 'APROBADA' },
          { pisoId: 'piso-destino-uuid', estado: 'PENDIENTE' },
        ],
      }])

      render(<AdministradorPisoPlanificacion />)
      expect(await screen.findByText('✓ Aprobado')).toBeInTheDocument()
      expect(screen.getByText('Pendiente')).toBeInTheDocument()
    })

    it('18. propia PENDIENTE -> muestra botones Aprobar / Rechazar', async () => {
      vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([{
        id: 'aggregate-1', carreraId: 'carrera-uuid', periodoId: 'periodo-uuid',
        estado: 'APROBADA', bloques: [plan('plan-1')], revisiones: [],
        pisoGestionadoId: 'piso-origen-uuid',
      }])
      vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([solicitudBase])

      render(<AdministradorPisoPlanificacion />)
      expect(await screen.findByRole('button', { name: 'Aprobar cambio' })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Rechazar cambio' })).toBeInTheDocument()
    })

    it('19. propia APROBADA -> sin botones + "Aprobado"', async () => {
      vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([{
        id: 'aggregate-1', carreraId: 'carrera-uuid', periodoId: 'periodo-uuid',
        estado: 'APROBADA', bloques: [plan('plan-1')], revisiones: [],
        pisoGestionadoId: 'piso-origen-uuid',
      }])
      vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([{
        ...solicitudBase,
        revisiones: [
          { pisoId: 'piso-origen-uuid', estado: 'APROBADA' },
          { pisoId: 'piso-destino-uuid', estado: 'PENDIENTE' },
        ],
      }])

      render(<AdministradorPisoPlanificacion />)
      expect(await screen.findByText('✓ Aprobado')).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Aprobar cambio' })).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Rechazar cambio' })).not.toBeInTheDocument()
    })

    it('20. ajena pendiente -> muestra claramente "Pendiente"', async () => {
      vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([{
        id: 'aggregate-1', carreraId: 'carrera-uuid', periodoId: 'periodo-uuid',
        estado: 'APROBADA', bloques: [plan('plan-1')], revisiones: [],
        pisoGestionadoId: 'piso-origen-uuid',
      }])
      vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([{
        ...solicitudBase,
        revisiones: [
          { pisoId: 'piso-origen-uuid', estado: 'APROBADA' },
          { pisoId: 'piso-destino-uuid', estado: 'PENDIENTE' },
        ],
      }])

      render(<AdministradorPisoPlanificacion />)
      expect(await screen.findByText('Pendiente')).toBeInTheDocument()
      expect(screen.getByText(/El cambio se aplicará cuando todos los pisos requeridos lo aprueben/)).toBeInTheDocument()
    })

    it('21. solicitud RECHAZADA -> feedback notorio y sin botones', async () => {
      vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([{
        id: 'aggregate-1', carreraId: 'carrera-uuid', periodoId: 'periodo-uuid',
        estado: 'APROBADA', bloques: [plan('plan-1')], revisiones: [],
        pisoGestionadoId: 'piso-origen-uuid',
      }])
      vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([{
        ...solicitudBase,
        estado: 'RECHAZADA',
        resolucion: 'No hay disponibilidad en el horario propuesto',
        revisiones: [
          { pisoId: 'piso-origen-uuid', estado: 'APROBADA' },
          { pisoId: 'piso-destino-uuid', estado: 'RECHAZADA' },
        ],
      }])

      render(<AdministradorPisoPlanificacion />)
      expect(await screen.findByText('🔴 Cambio rechazado')).toBeInTheDocument()
      expect(screen.getByText(/El cambio solicitado no fue aplicado/)).toBeInTheDocument()
      expect(screen.getByText(/No hay disponibilidad en el horario propuesto/)).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Aprobar cambio' })).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Rechazar cambio' })).not.toBeInTheDocument()
    })

    it('22. solicitud APROBADA -> feedback notorio', async () => {
      vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([{
        id: 'aggregate-1', carreraId: 'carrera-uuid', periodoId: 'periodo-uuid',
        estado: 'APROBADA', bloques: [plan('plan-1')], revisiones: [],
        pisoGestionadoId: 'piso-origen-uuid',
      }])
      vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([{
        ...solicitudBase,
        estado: 'APROBADA',
        revisiones: [
          { pisoId: 'piso-origen-uuid', estado: 'APROBADA' },
          { pisoId: 'piso-destino-uuid', estado: 'APROBADA' },
        ],
      }])

      render(<AdministradorPisoPlanificacion />)
      expect(await screen.findByText('🟢 Cambio aprobado')).toBeInTheDocument()
      expect(screen.getByText(/El cambio fue aprobado por todos los pisos involucrados/)).toBeInTheDocument()
    })

    it('23. tras aprobación completa, bloque desaparece de origen', async () => {
      // Piso origen ve la planificación, pero la lista de bloques en su piso ya no contiene el bloque migrado
      vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([{
        id: 'aggregate-1', carreraId: 'carrera-uuid', periodoId: 'periodo-uuid',
        estado: 'APROBADA', bloques: [], revisiones: [],
        pisoGestionadoId: 'piso-origen-uuid',
      }])
      vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([])

      render(<AdministradorPisoPlanificacion />)
      expect(await screen.findByText('0 bloques en su piso')).toBeInTheDocument()
      expect(screen.queryByText('Programación Web')).not.toBeInTheDocument()
    })

    it('24. bloque aparece en destino', async () => {
      // Piso destino ahora recibe en su lista agregada el bloque migrado con lab-destino
      const bloqueMigrado: operational.Planificacion = {
        id: 'plan-1', // MISMO ID DE BLOQUE
        planificacionId: 'aggregate-1',
        periodoId: 'periodo-uuid',
        carreraId: 'carrera-uuid',
        materiaId: 'materia-uuid',
        docenteId: 'docente-uuid',
        laboratorioId: 'lab-destino', // Ahora en lab de piso destino
        diaSemana: 'LUNES',
        horaInicio: '07:30',
        horaFin: '09:30',
        estado: 'CONFIRMADA',
        observacion: null,
        version: 1,
      }

      vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([{
        id: 'aggregate-1', carreraId: 'carrera-uuid', periodoId: 'periodo-uuid',
        estado: 'APROBADA', bloques: [bloqueMigrado], revisiones: [],
        pisoGestionadoId: 'piso-destino-uuid',
      }])
      vi.mocked(operational.listarSolicitudesCambio).mockResolvedValue([])

      render(<AdministradorPisoPlanificacion />)
      expect(await screen.findByText('1 bloques en su piso')).toBeInTheDocument()
      expect(screen.getByText('Programación Web')).toBeInTheDocument()
      expect(screen.getByText('LAB-P1-02')).toBeInTheDocument()
    })
  })
})
