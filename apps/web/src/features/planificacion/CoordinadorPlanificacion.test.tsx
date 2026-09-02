import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as academico from '../../services/academicoApi'
import * as api from '../../services/operationalApi'
import { CoordinadorPlanificacion } from './CoordinadorPlanificacion'

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

function preparar(
  items: api.Planificacion[] = [base, segunda],
  estado: api.EstadoPlanificacionAgregada = 'BORRADOR',
) {
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
  vi.mocked(academico.obtenerPeriodos).mockResolvedValue([{
    id: 'periodo-1',
    codigo: 'PPA-2026-2027-C1',
    nombre: 'Ciclo académico Mayo–Septiembre',
    fechaInicio: '2026-08-01',
    fechaFin: '2027-01-31',
    estado: 'ACTIVO',
    ppaCodigo: 'REGULAR-2026-2027-PPA',
    ppaNombre: 'REGULAR - 2026-2027 PPA',
    cicloAcademico: 1,
  }])
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

function renderPage() {
  return render(
    <MemoryRouter>
      <CoordinadorPlanificacion />
    </MemoryRouter>,
  )
}

describe('CoordinadorPlanificacion', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    preparar()
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
  it('permite retirar una planificación en revisión', async () => {
    preparar([base], 'EN_REVISION')
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const user = userEvent.setup()
    renderPage()
    await user.click(
      await screen.findByRole('button', { name: 'Retirar para corregir' }),
    )
    await waitFor(() =>
      expect(api.retirarPlanificacionCompleta).toHaveBeenCalledWith(
        'aggregate-1',
      ),
    )
  })
})
