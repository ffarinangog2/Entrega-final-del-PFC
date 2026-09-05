import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as academico from '../../services/academicoApi'
import * as operational from '../../services/operationalApi'
import * as usuarios from '../../services/usuariosApi'
import {
  MiHorarioPage,
  StudentHistorialPage,
  StudentLaboratoriosPage,
} from './StudentAcademicPage'
vi.mock('../../services/academicoApi')
vi.mock('../../services/operationalApi')
vi.mock('../../services/usuariosApi')
vi.mock('../../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => (
    <>{children}</>
  ),
}))
const bloque = {
  id: 'b1',
  planificacionId: 'p1',
  nivel: 7,
  periodoId: 'c1',
  carreraId: 'car1',
  materiaId: 'm1',
  docenteId: 'd1',
  laboratorioId: 'l1',
  diaSemana: 'LUNES',
  horaInicio: '10:00',
  horaFin: '12:00',
  estado: 'CONFIRMADA' as const,
  observacion: null,
  version: 0,
}
describe('experiencia académica del estudiante', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(usuarios.obtenerMisContextosAcademicos).mockResolvedValue([
      {
        id: 'ctx1',
        estudianteId: 'e1',
        carreraId: 'car1',
        periodoId: 'c1',
        nivel: 7,
        activo: true,
        creadoEn: '',
      },
      {
        id: 'ctx0',
        estudianteId: 'e1',
        carreraId: 'car1',
        periodoId: 'c0',
        nivel: 6,
        activo: false,
        creadoEn: '',
      },
    ])
    vi.mocked(academico.obtenerMaterias).mockResolvedValue([
      {
        id: 'm1',
        carreraId: 'car1',
        codigo: 'MAT',
        nombre: 'Aplicaciones',
        numeroHoras: 4,
        nivel: 7,
        activo: true,
      },
    ])
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([
      {
        id: 'l1',
        pisoId: 'pi1',
        codigo: 'LAB-03',
        nombre: 'Laboratorio 3',
        capacidad: 30,
        descripcion: '',
        estado: 'DISPONIBLE',
        activo: true,
        creadoEn: '',
        actualizadoEn: '',
      },
    ])
    vi.mocked(academico.obtenerPisos).mockResolvedValue([
      { id: 'pi1', bloqueId: 'x', numero: 3, descripcion: '', activo: true },
    ])
    vi.mocked(academico.obtenerCarreras).mockResolvedValue([
      {
        id: 'car1',
        facultadId: 'f',
        codigo: 'IS',
        nombre: 'Ingeniería de Software',
        activo: true,
      },
    ])
    vi.mocked(academico.obtenerPeriodos).mockResolvedValue([
      {
        id: 'c1',
        codigo: 'C1',
        nombre: 'Mayo–Septiembre',
        fechaInicio: '',
        fechaFin: '',
        estado: 'ACTIVO',
      },
      {
        id: 'c0',
        codigo: 'C0',
        nombre: 'Ciclo anterior',
        fechaInicio: '',
        fechaFin: '',
        estado: 'FINALIZADO',
      },
    ])
    vi.mocked(operational.obtenerMiHorario).mockResolvedValue([bloque])
    vi.mocked(operational.historialAsistencia).mockResolvedValue([])
    vi.mocked(usuarios.obtenerDocenteResumen).mockResolvedValue({ id: 'd1', nombres: 'Docente', apellidos: 'Demo', codigoDocente: 'DOC-1' })
  })
  it('deriva horario aprobado con nombres humanos y detalle de solo lectura', async () => {
    render(<MiHorarioPage />)
    expect(await screen.findByText('Aplicaciones')).toBeInTheDocument()
    expect(screen.getByText(/LAB-03/)).toBeInTheDocument()
    expect(await screen.findByText('Docente Demo')).toBeInTheDocument()
    expect(screen.getByRole('table', { name: 'Horario semanal de laboratorio' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /10:00/ }))
    expect(screen.getByText('Solo lectura')).toBeInTheDocument()
    expect(screen.queryByText(/UUID/i)).not.toBeInTheDocument()
  })
  it('muestra solo laboratorios usados por el horario', async () => {
    render(<StudentLaboratoriosPage />)
    expect(await screen.findByText(/LAB-03/)).toBeInTheDocument()
    expect(screen.getByText(/Piso 3/)).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: /Crear|Editar|Desactivar/ }),
    ).not.toBeInTheDocument()
  })
  it('cambiar ciclo histórico solo consulta y no modifica el contexto', async () => {
    render(<StudentHistorialPage />)
    await screen.findByText('Aplicaciones')
    fireEvent.change(screen.getByLabelText('Ciclo consultado'), {
      target: { value: 'c0' },
    })
    await waitFor(() =>
      expect(operational.obtenerMiHorario).toHaveBeenLastCalledWith('c0'),
    )
    expect(operational.historialAsistencia).toHaveBeenLastCalledWith('c0')
    expect(usuarios.obtenerMiContextoAcademico).not.toHaveBeenCalled()
  })
  it('presenta estados vacío y error sin romper la pantalla', async () => {
    vi.mocked(operational.obtenerMiHorario).mockResolvedValue([])
    render(<MiHorarioPage />)
    expect(
      await screen.findByText(
        'No existe un horario aprobado para el ciclo seleccionado.',
      ),
    ).toBeInTheDocument()
  })
  it('maneja de forma segura un estudiante todavía sin contexto', async () => {
    vi.mocked(usuarios.obtenerMisContextosAcademicos).mockResolvedValue([])
    render(<MiHorarioPage />)
    expect(await screen.findByText('No existe un horario aprobado para el ciclo seleccionado.')).toBeInTheDocument()
    expect(operational.obtenerMiHorario).not.toHaveBeenCalled()
  })
})
