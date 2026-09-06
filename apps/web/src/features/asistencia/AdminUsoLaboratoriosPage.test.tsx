import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '../../services/operationalApi'
import * as academico from '../../services/academicoApi'
import { AdminUsoLaboratoriosPage } from './AdminUsoLaboratoriosPage'

vi.mock('../../services/operationalApi')
vi.mock('../../services/academicoApi')
vi.mock('../../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dashboard-layout">{children}</div>
  ),
}))

const mockLaboratorio: academico.Laboratorio = {
  id: 'lab-uuid-1',
  pisoId: 'piso-uuid-1',
  codigo: 'LAB-REDES',
  nombre: 'Laboratorio de Redes',
  capacidad: 25,
  descripcion: 'Piso 2',
  estado: 'DISPONIBLE',
  activo: true,
  creadoEn: '2026-01-01T00:00:00Z',
  actualizadoEn: '2026-01-01T00:00:00Z',
}

const mockMateria: academico.Materia = {
  id: 'materia-uuid-1',
  carreraId: 'carrera-uuid-1',
  codigo: 'RED-101',
  nombre: 'Redes y Telecomunicaciones',
  numeroHoras: 4,
  activo: true,
}

const mockPeriodo: academico.PeriodoLectivo = {
  id: 'periodo-uuid-1',
  codigo: '2026-A',
  nombre: 'Periodo 2026-A',
  fechaInicio: '2026-03-01',
  fechaFin: '2026-08-31',
  estado: 'ACTIVO',
}

const mockUso: api.SesionAsistencia = {
  id: 'uso-uuid-1',
  reservaId: null,
  bloqueId: 'bloque-uuid-1',
  fechaClase: '2026-09-08',
  abiertaEn: '2026-09-08T08:30:00Z',
  expiraEn: '2026-09-08T08:45:00Z',
  cerradaEn: '2026-09-08T10:30:00Z',
  estado: 'CERRADA',
  token: null,
  temaActividad: 'Práctica de subnetting y VLANs',
  observacionUso: 'Todo el equipamiento quedó operativo',
  carreraId: 'carrera-uuid-1',
  periodoId: 'periodo-uuid-1',
  nivel: 4,
  materiaId: 'materia-uuid-1',
  docenteId: 'docente-uuid-1',
  laboratorioId: 'lab-uuid-1',
  pisoId: 'piso-uuid-1',
  diaSemana: 'MARTES',
  horaInicio: '08:30',
  horaFin: '10:30',
  esperados: 25,
  presentes: 22,
  ausentes: 3,
}

const mockParticipantes: api.ParticipanteUso[] = [
  {
    id: 'part-1',
    sesionId: 'uso-uuid-1',
    estudiantePerfilId: 'estudiante-perfil-1',
    estado: 'PRESENTE',
    registradaEn: '2026-09-08T08:35:00Z',
    registradoPorPerfilId: 'estudiante-perfil-1',
    observacion: null,
  },
  {
    id: 'part-2',
    sesionId: 'uso-uuid-1',
    estudiantePerfilId: 'estudiante-perfil-2',
    estado: 'AUSENTE',
    registradaEn: null,
    registradoPorPerfilId: null,
    observacion: null,
  },
]

describe('AdminUsoLaboratoriosPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([mockLaboratorio])
    vi.mocked(academico.obtenerMaterias).mockResolvedValue([mockMateria])
    vi.mocked(academico.obtenerPeriodos).mockResolvedValue([mockPeriodo])
    vi.mocked(api.listarUsosPiso).mockResolvedValue([mockUso])
    vi.mocked(api.listarParticipantesUsoPiso).mockResolvedValue(mockParticipantes)
  })

  it('1. Renderiza el listado de usos de laboratorio del piso', async () => {
    render(<AdminUsoLaboratoriosPage />)

    expect(
      await screen.findByRole('heading', { name: 'Registro de uso de laboratorios' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Administración operativa del piso')).toBeInTheDocument()
    expect(screen.getByRole('table')).toBeInTheDocument()
  })

  it('2. Muestra fecha, horario, laboratorio, materia/nivel y estado', async () => {
    render(<AdminUsoLaboratoriosPage />)

    const tabla = await screen.findByRole('table')
    expect(within(tabla).getByText('2026-09-08')).toBeInTheDocument()
    expect(within(tabla).getByText('08:30–10:30')).toBeInTheDocument()
    expect(within(tabla).getByText('LAB-REDES')).toBeInTheDocument()
    expect(within(tabla).getByText('Redes y Telecomunicaciones')).toBeInTheDocument()
    expect(within(tabla).getByText(/carrera-.*4°/)).toBeInTheDocument()
    expect(within(tabla).getByText('CERRADA')).toBeInTheDocument()
  })

  it('3. Muestra esperados, presentes y no registrados/ausentes según respuesta API', async () => {
    render(<AdminUsoLaboratoriosPage />)

    const tabla = await screen.findByRole('table')
    expect(within(tabla).getByText('25')).toBeInTheDocument()
    expect(within(tabla).getByText('22')).toBeInTheDocument()
    expect(within(tabla).getByText('3')).toBeInTheDocument() // 25 - 22
  })

  it('4. Permite abrir el detalle de un registro al pulsar la fila', async () => {
    render(<AdminUsoLaboratoriosPage />)

    const fila = await screen.findByText('2026-09-08')
    fireEvent.click(fila)

    await waitFor(() => {
      expect(api.listarParticipantesUsoPiso).toHaveBeenCalledWith('uso-uuid-1')
    })
    expect(
      await screen.findByRole('heading', { name: 'Detalle del registro de uso' }),
    ).toBeInTheDocument()
  })

  it('5. El detalle muestra tema/actividad, observación y lista de participantes', async () => {
    render(<AdminUsoLaboratoriosPage />)

    const fila = await screen.findByText('2026-09-08')
    fireEvent.click(fila)

    expect(
      await screen.findByText('Práctica de subnetting y VLANs'),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Todo el equipamiento quedó operativo'),
    ).toBeInTheDocument()
    expect(screen.getByText(/estudiante-perfil-1/)).toBeInTheDocument()
    expect(screen.getByText(/estudiante-perfil-2/)).toBeInTheDocument()
  })

  it('6. Muestra estado vacío cuando no existen registros', async () => {
    vi.mocked(api.listarUsosPiso).mockResolvedValue([])

    render(<AdminUsoLaboratoriosPage />)

    expect(
      await screen.findByText(
        'No existen registros de uso para los filtros seleccionados.',
      ),
    ).toBeInTheDocument()
  })

  it('7. Muestra mensaje de error accesible si falla la consulta (Error y no-Error)', async () => {
    vi.mocked(api.listarUsosPiso).mockRejectedValueOnce(
      new Error('Error al conectar con el servicio de reservas'),
    )

    const { unmount } = render(<AdminUsoLaboratoriosPage />)
    const alerta = await screen.findByRole('alert')
    expect(alerta).toHaveTextContent('Error al conectar con el servicio de reservas')
    unmount()

    // Error no estándar (string)
    vi.mocked(api.listarUsosPiso).mockRejectedValueOnce('fallo de red')
    render(<AdminUsoLaboratoriosPage />)
    expect(await screen.findByRole('alert')).toHaveTextContent('No fue posible cargar los registros')
  })

  it('8. Verifica que la pantalla sea de solo lectura y no exponga acciones de modificación', async () => {
    render(<AdminUsoLaboratoriosPage />)

    await screen.findByText('2026-09-08')
    const fila = screen.getByText('2026-09-08')
    fireEvent.click(fila)
    await screen.findByRole('heading', { name: 'Detalle del registro de uso' })

    // No marcar estudiante / no registrar presencia
    expect(screen.queryByRole('button', { name: /marcar/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /asistencia/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /presente/i })).not.toBeInTheDocument()

    // No editar tema u observación
    expect(screen.queryByRole('button', { name: /editar/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /guardar/i })).not.toBeInTheDocument()

    // No abrir ni cerrar sesión de uso
    expect(screen.queryByRole('button', { name: /abrir/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /cerrar/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /habilitar/i })).not.toBeInTheDocument()

    // No modificar planificación (botones de aprobación/rechazo)
    expect(screen.queryByRole('button', { name: /aprobar/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /rechazar/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /propuesta/i })).not.toBeInTheDocument()
  })

  it('9. Permite interactuar con todos los filtros y consultar con los valores seleccionados', async () => {
    render(<AdminUsoLaboratoriosPage />)
    await screen.findByRole('heading', { name: 'Registro de uso de laboratorios' })

    const fechaInput = screen.getByLabelText('Fecha')
    fireEvent.change(fechaInput, { target: { value: '2026-09-08' } })

    const periodoSelect = screen.getByLabelText('Período')
    fireEvent.change(periodoSelect, { target: { value: 'periodo-uuid-1' } })

    const labSelect = screen.getByLabelText('Laboratorio')
    fireEvent.change(labSelect, { target: { value: 'lab-uuid-1' } })

    const materiaSelect = screen.getByLabelText('Materia')
    fireEvent.change(materiaSelect, { target: { value: 'materia-uuid-1' } })

    const estadoSelect = screen.getByLabelText('Estado')
    fireEvent.change(estadoSelect, { target: { value: 'ABIERTA' } })

    const aplicarBtn = screen.getByRole('button', { name: 'Aplicar filtros' })
    fireEvent.click(aplicarBtn)

    await waitFor(() => {
      expect(api.listarUsosPiso).toHaveBeenCalledWith({
        fecha: '2026-09-08',
        periodoId: 'periodo-uuid-1',
        laboratorioId: 'lab-uuid-1',
        materiaId: 'materia-uuid-1',
        estado: 'ABIERTA',
      })
    })
  })

  it('10. Muestra valores por defecto cuando campos del uso y detalle son nulos', async () => {
    const usoIncompleto: api.SesionAsistencia = {
      id: 'uso-inc-1',
      reservaId: null,
      bloqueId: 'bloque-uuid-2',
      fechaClase: '2026-09-09',
      abiertaEn: '2026-09-09T08:00:00Z',
      expiraEn: '2026-09-09T08:15:00Z',
      cerradaEn: null,
      estado: 'ABIERTA',
      token: null,
      temaActividad: null,
      observacionUso: null,
      carreraId: null,
      periodoId: null,
      nivel: 1,
      materiaId: null,
      docenteId: 'docente-1',
      laboratorioId: null,
      pisoId: 'piso-uuid-1',
      diaSemana: 'MIERCOLES',
      horaInicio: '08:00',
      horaFin: '10:00',
      esperados: undefined,
      presentes: undefined,
      ausentes: undefined,
    }

    vi.mocked(api.listarUsosPiso).mockResolvedValue([usoIncompleto])
    vi.mocked(api.listarParticipantesUsoPiso).mockResolvedValue([])

    render(<AdminUsoLaboratoriosPage />)

    const tabla = await screen.findByRole('table')
    const tbody = tabla.querySelector('tbody')!
    expect(within(tbody).getByText('Laboratorio')).toBeInTheDocument()
    expect(within(tbody).getByText('Materia')).toBeInTheDocument()

    // Abrir detalle
    fireEvent.click(within(tabla).getByText('2026-09-09'))
    expect(await screen.findByText('Pendiente de registrar')).toBeInTheDocument()
    expect(screen.getByText('Sin observaciones')).toBeInTheDocument()
    expect(screen.getByText('En curso')).toBeInTheDocument()
  })
})
