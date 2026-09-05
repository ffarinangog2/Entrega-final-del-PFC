import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as academico from '../../services/academicoApi'
import * as operational from '../../services/operationalApi'
import { IncidentesPage } from './IncidentesPage'

vi.mock('../../services/academicoApi')
vi.mock('../../services/operationalApi')
vi.mock('../../auth', async (original) => ({
  ...(await original<typeof import('../../auth')>()),
  useAuth: () => ({ usuario: { roles: ['ADMINISTRADOR'], permisos: ['INCIDENTE_CREAR', 'INCIDENTE_GESTIONAR'] } }),
}))
vi.mock('../../components/DashboardLayout', () => ({ DashboardLayout: ({ children }: { children: React.ReactNode }) => <>{children}</> }))

describe('IncidentesPage para ADMIN', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([{ id: 'l1', pisoId: 'p1', codigo: 'LAB-01', nombre: 'Software', capacidad: 30, descripcion: '', estado: 'DISPONIBLE', activo: true, creadoEn: '', actualizadoEn: '' }])
    vi.mocked(operational.listarIncidentes).mockResolvedValue([{ id: 'i1', laboratorioEquipo: 'LAB-01', descripcion: 'Sin red', prioridad: 'ALTA', fecha: '2026-09-03', estado: 'REPORTADO', creadoEn: '' }])
  })
  it('usa laboratorio institucional y permite filtros globales', async () => {
    render(<IncidentesPage />)
    expect(await screen.findByText('Sin red')).toBeInTheDocument()
    expect(screen.getByLabelText('Laboratorio o equipo').tagName).toBe('SELECT')
    fireEvent.change(screen.getAllByLabelText('Laboratorio')[0], { target: { value: 'LAB-01' } })
    fireEvent.change(screen.getAllByLabelText('Prioridad')[1], { target: { value: 'ALTA' } })
    fireEvent.change(screen.getByLabelText('Estado'), { target: { value: 'REPORTADO' } })
    expect(screen.getByText('Sin red')).toBeInTheDocument()
    fireEvent.change(screen.getAllByLabelText('Prioridad')[1], { target: { value: 'BAJA' } })
    expect(screen.getByText('No existen incidentes.')).toBeInTheDocument()
  })
})
