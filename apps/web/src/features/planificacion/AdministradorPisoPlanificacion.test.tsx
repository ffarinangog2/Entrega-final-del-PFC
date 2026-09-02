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
    vi.mocked(operational.listarPlanificaciones).mockResolvedValue([
      plan('plan-1'),
      plan('plan-2'),
    ])
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
      expect(operational.accionPlanificacion).toHaveBeenCalledTimes(2),
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
      expect(operational.rechazarPlanificacion).toHaveBeenCalledTimes(2),
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
      expect(operational.proponerPlanificacion).toHaveBeenCalledWith(
        'plan-1',
        expect.objectContaining({
          observacion: 'Laboratorio en mantenimiento',
        }),
      ),
    )
  })

  it('muestra error controlado cuando falla el API', async () => {
    vi.mocked(operational.listarPlanificaciones).mockRejectedValue(
      new Error('No tiene un piso institucional asignado.'),
    )
    render(<AdministradorPisoPlanificacion />)
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No tiene un piso institucional asignado.',
    )
  })

  it('mantiene etiquetas coherentes de estado del paquete', () => {
    expect(estadoPaquete([plan('p', 'ENVIADA')])).toBe('Pendiente de revisión')
    expect(estadoPaquete([plan('p', 'CONFIRMADA')])).toBe('Aprobada')
    expect(estadoPaquete([plan('p', 'PROPUESTA_CAMBIO')])).toBe(
      'Devuelta con observaciones',
    )
  })
})
