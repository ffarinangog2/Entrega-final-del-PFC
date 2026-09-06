import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as academico from '../../services/academicoApi'
import * as api from '../../services/operationalApi'
import { CoordinadorPlanificacion } from './CoordinadorPlanificacion'
import { laboratoriosDelPiso, pisoDelLaboratorio } from './planificacionLaboratorioFilter'
import { AcademicPeriodContext, type AcademicPeriodContextValue } from '../../academicPeriodContext'

vi.mock('../../services/academicoApi')
vi.mock('../../services/operationalApi')
vi.mock('../../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => (
    <>{children}</>
  ),
}))

const base: api.Planificacion = {
  id: 'plan-1',
  planificacionId: 'aggregate-1',
  nivel: 1,
  periodoId: 'periodo-1',
  carreraId: 'carrera-1',
  materiaId: 'materia-1',
  docenteId: 'docente-1',
  laboratorioId: 'lab-1',
  diaSemana: 'LUNES',
  horaInicio: '07:30',
  horaFin: '09:30',
  estado: 'BORRADOR',
  observacion: null,
  version: 0,
}
const segunda: api.Planificacion = {
  ...base,
  id: 'plan-2',
  materiaId: 'materia-2',
  laboratorioId: 'lab-2',
  diaSemana: 'MARTES',
  horaInicio: '09:30',
  horaFin: '11:30',
}
const periodoActual: academico.PeriodoLectivo = {
  id: 'periodo-1',
  codigo: 'PPA-2026-2027-C1',
  nombre: 'Ciclo académico Mayo–Septiembre',
  fechaInicio: '2026-05-01',
  fechaFin: '2026-09-18',
  estado: 'PLANIFICADO',
  ppaCodigo: 'REGULAR-2026-2027-PPA',
  ppaNombre: 'REGULAR - 2026-2027 PPA',
  cicloAcademico: 1,
}

function preparar(
  items: api.Planificacion[] = [base, segunda],
  estado: api.EstadoPlanificacionAgregada = 'BORRADOR',
) {
  vi.mocked(api.listarSolicitudesRetiro).mockResolvedValue([])
  vi.mocked(api.obtenerDisponibilidadPlanificacion).mockResolvedValue({ docentesOcupados: [], laboratoriosOcupados: [] })
  const aggregate: api.PlanificacionAgregada = {
    id: 'aggregate-1',
    carreraId: 'carrera-1',
    periodoId: 'periodo-1',
    estado,
    bloques: items,
    revisiones: [],
  }
  vi.mocked(api.listarPlanificacionesAgregadas).mockResolvedValue(
    items.length === 0 ? [] : [aggregate],
  )
  vi.mocked(academico.obtenerCarreras).mockResolvedValue([
    {
      id: 'carrera-1',
      facultadId: 'facultad-1',
      codigo: 'IS',
      nombre: 'Ingeniería de Software',
      activo: true,
    },
  ])
  vi.mocked(academico.obtenerMaterias).mockResolvedValue([
    {
      id: 'materia-1',
      carreraId: 'carrera-1',
      codigo: 'PROG',
      nombre: 'Programación',
      numeroHoras: 64,
      activo: true,
    },
    {
      id: 'materia-2',
      carreraId: 'carrera-1',
      codigo: 'BDD',
      nombre: 'Bases de Datos',
      numeroHoras: 64,
      activo: true,
    },
  ])
  vi.mocked(academico.obtenerDocentesPlanificacion).mockResolvedValue([
    {
      id: 'docente-1',
      perfilId: 'perfil-1',
      codigoDocente: 'DOC-CARLOS',
      activo: true,
    },
  ])
  vi.mocked(academico.obtenerPisos).mockResolvedValue([
    { id: 'piso-1', bloqueId: 'bloque-1', numero: 1, descripcion: 'Piso 1', activo: true },
    { id: 'piso-2', bloqueId: 'bloque-1', numero: 2, descripcion: 'Piso 2', activo: true },
  ])
  vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([
    {
      id: 'lab-1',
      pisoId: 'piso-2',
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
      id: 'lab-2',
      pisoId: 'piso-1',
      codigo: 'LAB-02',
      nombre: 'Laboratorio de Bases de Datos',
      capacidad: 30,
      descripcion: '',
      estado: 'DISPONIBLE',
      activo: true,
      creadoEn: '',
      actualizadoEn: '',
    },
  ])
  vi.mocked(api.crearPlanificacion).mockImplementation(async (body) => ({
    ...base,
    ...body,
    id: 'plan-nueva',
    estado: 'BORRADOR',
    observacion: body.observacion || null,
    version: 0,
  }))
  vi.mocked(api.editarPlanificacion).mockResolvedValue(base)
  vi.mocked(api.accionPlanificacion).mockResolvedValue({
    ...base,
    estado: 'ENVIADA',
  })
  vi.mocked(api.iniciarPlanificacion).mockResolvedValue(aggregate)
  vi.mocked(api.enviarPlanificacionCompleta).mockResolvedValue({
    ...aggregate,
    estado: 'EN_REVISION',
  })
  vi.mocked(api.retirarPlanificacionCompleta).mockResolvedValue({
    ...aggregate,
    estado: 'BORRADOR',
  })
}

function renderPage(periodContext: AcademicPeriodContextValue = { periodos: [periodoActual], periodoVigente: periodoActual, periodoSeleccionado: periodoActual, seleccionarPeriodo: vi.fn(), cargando: false }) {
  return render(
    <MemoryRouter>
      <AcademicPeriodContext.Provider value={periodContext}>
        <CoordinadorPlanificacion />
      </AcademicPeriodContext.Provider>
    </MemoryRouter>,
  )
}

describe('CoordinadorPlanificacion', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    preparar()
  })

  it('no muestra ausencia de período ni carga catálogos mientras el período está cargando', () => {
    renderPage({ periodos: [], periodoVigente: null, periodoSeleccionado: null, seleccionarPeriodo: vi.fn(), cargando: true })

    expect(screen.getByRole('heading', { name: 'Planificación semanal' }).nextElementSibling)
      .toHaveTextContent('Cargando período…')
    expect(screen.queryByText('Sin período académico actual')).not.toBeInTheDocument()
    expect(api.listarPlanificacionesAgregadas).not.toHaveBeenCalled()
  })

  it('muestra ausencia real cuando terminó la consulta sin período vigente', async () => {
    renderPage({ periodos: [], periodoVigente: null, periodoSeleccionado: null, seleccionarPeriodo: vi.fn(), cargando: false })

    expect(await screen.findAllByText('Sin período académico actual')).not.toHaveLength(0)
  })

  it('muestra la planificación propia con catálogos humanos y sin UUID visibles', async () => {
    renderPage()
    expect(
      await screen.findByText(
        (_, element) =>
          element?.tagName === 'SPAN' &&
          element.textContent?.includes('Ingeniería de Software') === true,
      ),
    ).toBeInTheDocument()
    expect(screen.getByText('Programación')).toBeInTheDocument()
    expect(screen.getAllByText('DOC-CARLOS')).toHaveLength(2)
    expect(screen.getByText('LAB-01')).toBeInTheDocument()
    expect(screen.queryByText('materia-1')).not.toBeInTheDocument()
    expect(screen.getByText(/Ingeniería de Software · REGULAR 2026-2027 PPA/)).toBeInTheDocument()
    expect(screen.queryByLabelText('Periodo')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Período de consulta')).not.toBeInTheDocument()
    expect(screen.queryByText(/Periodo Lectivo 2026-[AB]/)).not.toBeInTheDocument()
    expect(academico.obtenerPeriodos).not.toHaveBeenCalled()
  })

  it('guarda una nueva asignación como borrador', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('Programación')
    await user.click(
      screen.getByRole('button', { name: 'Agregar MIERCOLES 10:30' }),
    )
    await user.selectOptions(screen.getByLabelText('Materia'), 'materia-2')
    await user.selectOptions(screen.getByLabelText('Docente'), 'docente-1')
    await user.selectOptions(screen.getByLabelText('Laboratorio'), 'lab-2')
    await user.click(screen.getByRole('button', { name: 'Guardar' }))
    await waitFor(() =>
      expect(api.crearPlanificacion).toHaveBeenCalledWith(
        expect.objectContaining({
          diaSemana: 'MIERCOLES',
          materiaId: 'materia-2',
        }),
      ),
    )
    expect(screen.getAllByText('Bases de Datos')).toHaveLength(2)
  })

  it('filtra laboratorios por piso sin añadir el piso a la planificación', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('Programación')
    await user.click(screen.getByRole('button', { name: 'Agregar MIERCOLES 10:30' }))

    const laboratorio = screen.getByLabelText('Laboratorio')
    expect(laboratorio).toHaveTextContent('LAB-01')
    expect(laboratorio).toHaveTextContent('LAB-02')

    await user.selectOptions(screen.getByLabelText('Piso'), 'piso-2')
    expect(laboratorio).toHaveTextContent('LAB-01')
    expect(laboratorio).not.toHaveTextContent('LAB-02')

    await user.selectOptions(screen.getByLabelText('Piso'), '')
    expect(laboratorio).toHaveTextContent('LAB-01')
    expect(laboratorio).toHaveTextContent('LAB-02')
  })

  it('deriva el piso real al editar y limpia un laboratorio incompatible', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByText(/Programaci/)
    await user.click(screen.getAllByRole('button', { name: 'Editar' })[0])

    expect(screen.getByLabelText('Piso')).toHaveValue('piso-2')
    expect(screen.getByLabelText('Laboratorio')).toHaveValue('lab-1')

    await user.selectOptions(screen.getByLabelText('Piso'), 'piso-1')
    expect(screen.getByLabelText('Laboratorio')).toHaveValue('')
    expect(screen.getByLabelText('Laboratorio')).not.toHaveTextContent('LAB-01')
    expect(screen.getByLabelText('Laboratorio')).toHaveTextContent('LAB-02')
  })

  it('cambia de nivel y presenta únicamente sus bloques', async () => {
    preparar([base, { ...segunda, nivel: 2 }])
    const user = userEvent.setup()
    renderPage()
    await screen.findByText(/Programaci/)

    const nivelDos = screen
      .getAllByRole('button')
      .find((button) => button.textContent === '2°')
    expect(nivelDos).toBeDefined()
    await user.click(nivelDos!)

    expect(screen.getByText('Bases de Datos')).toBeInTheDocument()
    expect(screen.queryByText(/Programaci/)).not.toBeInTheDocument()
    expect(nivelDos).toHaveAttribute('aria-pressed', 'true')
  })

  it('inicia una planificación vacía y habilita la cuadrícula', async () => {
    preparar([])
    const user = userEvent.setup()
    renderPage()
    await user.click(
      await screen.findByRole('button', { name: 'Iniciar planificación' }),
    )
    expect(
      screen.getByRole('button', { name: 'Agregar LUNES 07:30' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Materias disponibles')).toBeInTheDocument()
  })

  it('edita una asignación existente', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('Programación')
    await user.click(screen.getAllByRole('button', { name: 'Editar' })[0])
    fireEvent.change(screen.getByLabelText('Hora fin'), {
      target: { value: '10:30' },
    })
    await user.click(screen.getByRole('button', { name: 'Guardar' }))
    await waitFor(() =>
      expect(api.editarPlanificacion).toHaveBeenCalledWith(
        'plan-1',
        expect.objectContaining({ horaFin: '10:30' }),
      ),
    )
  })

  it('muestra un conflicto humano sin guardar', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('Programación')
    await user.click(
      screen.getByRole('button', { name: 'Agregar LUNES 08:30' }),
    )
    await user.selectOptions(screen.getByLabelText('Materia'), 'materia-2')
    await user.selectOptions(screen.getByLabelText('Docente'), 'docente-1')
    await user.selectOptions(screen.getByLabelText('Laboratorio'), 'lab-1')
    await user.click(screen.getByRole('button', { name: 'Guardar' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'LAB-01 no está disponible',
    )
    expect(api.crearPlanificacion).not.toHaveBeenCalled()
  })

  it('realiza una sola confirmación y envía todos los bloques del borrador', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('Programación')
    await user.click(
      screen.getByRole('button', { name: 'Enviar planificación completa' }),
    )
    expect(
      screen.getByRole('heading', { name: 'Confirmar envío' }),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Bloques planificados').nextSibling,
    ).toHaveTextContent('2')
    await user.click(screen.getByRole('button', { name: 'Confirmar envío' }))
    await waitFor(() =>
      expect(api.enviarPlanificacionCompleta).toHaveBeenCalledTimes(1),
    )
    expect(api.enviarPlanificacionCompleta).toHaveBeenCalledWith('aggregate-1')
  })

  it('muestra una planificación aprobada en consulta y controla errores API', async () => {
    preparar([{ ...base, estado: 'CONFIRMADA' }], 'APROBADA')
    const { unmount } = renderPage()
    expect(await screen.findByText('Aprobada')).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Enviar planificación completa' }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Editar' }),
    ).not.toBeInTheDocument()
    unmount()
    vi.mocked(api.listarPlanificacionesAgregadas).mockRejectedValue(
      new Error('Servicio temporalmente no disponible'),
    )
    renderPage()
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Servicio temporalmente no disponible',
    )
  })
  it('solicita con motivo el retiro de una planificaci?n en revisi?n', async () => {
    preparar([base], 'EN_REVISION')
    vi.mocked(api.crearSolicitudRetiro).mockResolvedValue({
      id: 'retiro-1', planificacionId: 'aggregate-1', solicitantePerfilId: 'perfil-1',
      motivo: 'Corregir asignaciones', estado: 'PENDIENTE', creadaEn: new Date().toISOString(),
      resueltaEn: null, pisosAprobados: 0, totalPisos: 2, decisiones: [],
    })
    const user = userEvent.setup()
    renderPage()
    await user.click(await screen.findByRole('button', { name: 'Solicitar retiro para editar' }))
    await user.type(screen.getByLabelText('Motivo'), 'Corregir asignaciones')
    await user.click(screen.getByRole('button', { name: 'Confirmar solicitud' }))
    await waitFor(() => expect(api.crearSolicitudRetiro)
      .toHaveBeenCalledWith('aggregate-1', 'Corregir asignaciones'))
  })


  it('al abrir modal de cambio de laboratorio muestra las opciones con nombre de piso resuelto del catálogo y mantiene el bloque original', async () => {
    preparar([{ ...base, estado: 'CONFIRMADA' }], 'APROBADA')
    vi.mocked(api.crearSolicitudCambio).mockResolvedValue({} as api.SolicitudCambio)
    const user = userEvent.setup()
    renderPage()

    const botonCambio = await screen.findByRole('button', { name: 'Solicitar cambio' })
    await user.click(botonCambio)

    expect(await screen.findByRole('heading', { name: 'Solicitar cambio' })).toBeInTheDocument()
    expect(
      screen.getByText('El horario aprobado seguirá vigente hasta que todos los pisos afectados autoricen la solicitud.'),
    ).toBeInTheDocument()

    const opcionLab1 = screen.getByRole('option', { name: 'Piso 2 · LAB-01 — DISPONIBLE' })
    const opcionLab2 = screen.getByRole('option', { name: 'Piso 1 · LAB-02 — DISPONIBLE' })
    expect(opcionLab1).toBeInTheDocument()
    expect(opcionLab2).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Laboratorio propuesto'), 'lab-2')
    await user.type(screen.getByLabelText('Motivo'), 'Reubicación por requerimiento técnico')
    await user.click(screen.getByRole('button', { name: 'Enviar solicitud' }))

    await waitFor(() =>
      expect(api.crearSolicitudCambio).toHaveBeenCalledWith(
        'aggregate-1',
        expect.objectContaining({
          bloqueId: 'plan-1',
          tipo: 'LABORATORIO',
          laboratorioId: 'lab-2',
          motivo: 'Reubicación por requerimiento técnico',
        }),
      ),
    )

    // El horario original del bloque se mantiene visible en la cuadrícula
    expect(screen.getByText('07:30–09:30')).toBeInTheDocument()
    expect(screen.getByText('Programación')).toBeInTheDocument()
  })
})

describe('filtro por pisoId real', () => {
  const pisos = [1, 2, 3, 4].map((numero) =>
    `35000000-0000-0000-0000-${String(numero).padStart(12, '0')}`,
  )
  const laboratorio = (id: string, pisoId: string, codigo: string): academico.Laboratorio => ({
    id, pisoId, codigo, nombre: codigo, capacidad: 30, descripcion: '',
    estado: 'DISPONIBLE', activo: true, creadoEn: '', actualizadoEn: '',
  })
  const laboratorios = [
    laboratorio('l1', pisos[0], 'LAB-P2-ENGANOSO'),
    laboratorio('l2', pisos[1], 'LAB-P1-03'),
    laboratorio('l3', pisos[1], 'LAB-03'),
    laboratorio('l4', pisos[2], 'LAB-P3-01'),
    laboratorio('l5', pisos[3], 'LAB-P4-01'),
  ]

  it.each([
    [pisos[0], ['LAB-P2-ENGANOSO']],
    [pisos[1], ['LAB-P1-03', 'LAB-03']],
    [pisos[2], ['LAB-P3-01']],
    [pisos[3], ['LAB-P4-01']],
  ])('muestra solo laboratorios cuyo pisoId es %s', (pisoId, esperados) => {
    expect(laboratoriosDelPiso(laboratorios, pisoId).map((item) => item.codigo))
      .toEqual(esperados)
  })

  it('Piso 1 no incluye laboratorios asignados realmente a Piso 2', () => {
    expect(laboratoriosDelPiso(laboratorios, pisos[0]).map((item) => item.codigo))
      .not.toEqual(expect.arrayContaining(['LAB-P1-03', 'LAB-03']))
    expect(pisoDelLaboratorio(laboratorios, 'l2')).toBe(pisos[1])
  })
})
