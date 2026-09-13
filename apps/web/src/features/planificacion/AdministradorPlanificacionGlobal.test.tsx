import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as academico from '../../services/academicoApi'
import * as operational from '../../services/operationalApi'
import { AdministradorPlanificacionGlobal } from './AdministradorPlanificacionGlobal'

vi.mock('../../services/academicoApi')
vi.mock('../../services/operationalApi')
vi.mock('../../components/DashboardLayout', () => ({ DashboardLayout: ({ children }: { children: React.ReactNode }) => <>{children}</> }))

describe('AdministradorPlanificacionGlobal', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(academico.obtenerCarreras).mockResolvedValue([{ id: 'c1', facultadId: 'f1', codigo: 'IS', nombre: 'Ingeniería de Software', activo: true }])
    vi.mocked(academico.obtenerPeriodos).mockResolvedValue([{ id: 'pe1', codigo: 'C1', nombre: 'Mayo–Septiembre', fechaInicio: '2026-05-01', fechaFin: '2026-09-18', estado: 'ACTIVO', ppaNombre: 'REGULAR - 2026-2027 PPA', cicloAcademico: 1 }])
    vi.mocked(academico.obtenerMaterias).mockResolvedValue([{ id: 'm1', carreraId: 'c1', codigo: 'MAT', nombre: 'Programación', numeroHoras: 4, nivel: 5, activo: true }])
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([{ id: 'l1', pisoId: 'p1', codigo: 'LAB-01', nombre: 'Software', capacidad: 30, descripcion: '', estado: 'DISPONIBLE', activo: true, creadoEn: '', actualizadoEn: '' }])
    vi.mocked(academico.obtenerPisos).mockResolvedValue([{ id: 'p1', bloqueId: 'b1', numero: 1, descripcion: '', activo: true }])
    vi.mocked(operational.listarPlanificacionesAgregadas).mockResolvedValue([{ id: 'plan-1', carreraId: 'c1', periodoId: 'pe1', estado: 'EN_REVISION', revisiones: [{ id: 'r1', pisoId: 'p1', estado: 'PENDIENTE', observacion: null }], bloques: [{ id: 'bl1', planificacionId: 'plan-1', nivel: 5, periodoId: 'pe1', carreraId: 'c1', materiaId: 'm1', docenteId: 'd1', laboratorioId: 'l1', diaSemana: 'LUNES', horaInicio: '07:30', horaFin: '09:30', estado: 'ENVIADA', observacion: null, version: 0 }] }])
  })

  it('muestra planes agregados, revisiones por piso y detalle por nivel', async () => {
    render(<AdministradorPlanificacionGlobal />)
    expect(await screen.findAllByText('Ingeniería de Software')).toHaveLength(2)
    expect(screen.getByText(/Piso 1: PENDIENTE/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Ver detalle' }))
    expect(screen.getByText('Programación')).toBeInTheDocument()
    expect(screen.getByText('LAB-01 · Piso 1')).toBeInTheDocument()
  })

  it('filtra sin convertir ausencia en error', async () => {
    render(<AdministradorPlanificacionGlobal />)
    await screen.findAllByText('Ingeniería de Software')
    fireEvent.change(screen.getByLabelText('Estado'), { target: { value: 'APROBADA' } })
    expect(screen.getByText('No existen planes con estos filtros.')).toBeInTheDocument()
  })

  it('distingue un error de servicio de un resultado vacío', async () => {
    vi.mocked(operational.listarPlanificacionesAgregadas).mockRejectedValue(new Error('Servicio no disponible'))
    render(<AdministradorPlanificacionGlobal />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Servicio no disponible')
    expect(screen.queryByText('No existen planes con estos filtros.')).not.toBeInTheDocument()
  })
})
