import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from './reservasApi'
import * as academicoApi from '../../services/academicoApi'
import { NuevaSolicitudPage } from './NuevaSolicitudPage'

vi.mock('./reservasApi', async (original) => ({ ...(await original<typeof import('./reservasApi')>()), crearSolicitud: vi.fn(), consultarDisponibilidad: vi.fn() }))
vi.mock('../../services/academicoApi')
const authUser = { perfilId: 'perfil-1', roles: ['DOCENTE'], permisos: ['SOLICITUD_CREAR'] }
vi.mock('../../auth', async (original) => ({ ...(await original<typeof import('../../auth')>()), useAuth: () => ({ usuario: authUser }) }))
vi.mock('../../components/DashboardLayout', () => ({ DashboardLayout: ({ children }: { children: React.ReactNode }) => <>{children}</> }))

const lab = { id: 'lab-1', pisoId: 'p1', codigo: 'LAB-1', nombre: 'Redes', capacidad: 20, descripcion: '', estado: 'DISPONIBLE' as const, activo: true, creadoEn: '', actualizadoEn: '' }
const docente = { id: 'doc-1', perfilId: 'perfil-1', codigoDocente: 'DOC-01', activo: true }
const materia = { id: 'mat-1', carreraId: 'c1', codigo: 'MAT-1', nombre: 'Redes I', numeroHoras: 40, activo: true }
const periodo = { id: 'per-1', codigo: '2026-A', nombre: 'Primer período', fechaInicio: '', fechaFin: '', estado: 'ACTIVO' as const }
const horario = { id: 'h1', materiaId: 'mat-1', periodoLectivoId: 'per-1', laboratorioId: 'lab-1', docenteId: 'doc-1', diaSemana: 'LUNES', horaInicio: '08:00', horaFin: '10:00', paralelo: 'A', activo: true }
const solicitud = { id: 'sol-1', solicitanteId: 'perfil-1', docenteId: 'doc-1', laboratorioId: 'lab-1', materiaId: 'mat-1', periodoLectivoId: 'per-1', fechaReserva: '2099-08-20', horaInicio: '08:00', horaFin: '10:00', numeroParticipantes: 1, motivo: 'Clase práctica', observacion: '', estado: 'PENDIENTE' as const, propuestaFecha: null, propuestaHoraInicio: null, propuestaHoraFin: null, propuestaLaboratorioId: null, propuestaObservacion: null, reservaId: null, creadaEn: '', actualizadaEn: '', version: 0 }

function renderForm() { render(<MemoryRouter initialEntries={['/reservas/nueva']}><Routes><Route path="/reservas/nueva" element={<NuevaSolicitudPage />} /><Route path="/solicitudes/:id" element={<p>Detalle creado</p>} /></Routes></MemoryRouter>) }
async function completar() {
  fireEvent.change(await screen.findByLabelText('Materia'), { target: { value: 'mat-1' } })
  fireEvent.change(screen.getByLabelText('Laboratorio'), { target: { value: 'lab-1' } })
  fireEvent.change(screen.getByLabelText('Fecha'), { target: { value: '2099-08-20' } })
  fireEvent.change(screen.getByLabelText('Hora inicio'), { target: { value: '08:00' } })
  fireEvent.change(screen.getByLabelText('Hora fin'), { target: { value: '10:00' } })
  fireEvent.change(screen.getByLabelText('Motivo'), { target: { value: 'Clase práctica' } })
}

describe('NuevaSolicitudPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(academicoApi.obtenerLaboratorios).mockResolvedValue([lab])
    vi.mocked(academicoApi.obtenerMaterias).mockResolvedValue([materia])
    vi.mocked(academicoApi.obtenerPeriodoActual).mockResolvedValue(periodo)
    vi.mocked(academicoApi.obtenerDocentePorPerfil).mockResolvedValue(docente)
    vi.mocked(academicoApi.obtenerHorariosDocente).mockResolvedValue([horario])
  })

  it('resuelve docente y carga selectores humanos sin inputs UUID manuales', async () => {
    renderForm()
    expect(await screen.findByRole('option', { name: 'MAT-1 — Redes I' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'LAB-1 — Redes' })).toBeInTheDocument()
    expect(screen.getByDisplayValue('2026-A — Primer período')).toBeInTheDocument()
    expect(screen.queryByLabelText('Docente ID')).not.toBeInTheDocument()
    expect(academicoApi.obtenerDocentePorPerfil).toHaveBeenCalledWith('perfil-1')
    expect(academicoApi.obtenerHorariosDocente).toHaveBeenCalledWith('doc-1')
  })

  it('envía IDs resueltos con Idempotency-Key y navega al detalle', async () => {
    vi.mocked(api.crearSolicitud).mockResolvedValue(solicitud)
    renderForm(); await completar(); fireEvent.click(screen.getByRole('button', { name: 'Crear solicitud' }))
    await screen.findByText('Detalle creado')
    const [body, key] = vi.mocked(api.crearSolicitud).mock.calls[0]
    expect(body).toMatchObject({ solicitanteId: 'perfil-1', docenteId: 'doc-1', materiaId: 'mat-1', periodoLectivoId: 'per-1', laboratorioId: 'lab-1' })
    expect(key).toBeTruthy()
  })

  it('comprueba disponibilidad con una explicación humana', async () => {
    vi.mocked(api.consultarDisponibilidad).mockResolvedValue({ laboratorioId: 'lab-1', fecha: '2099-08-20', horaInicio: '08:00', horaFin: '10:00', disponible: false, motivo: 'Existe una reserva' })
    renderForm(); await completar(); fireEvent.click(screen.getByRole('button', { name: 'Comprobar disponibilidad' }))
    expect(await screen.findByText('No disponible: Existe una reserva')).toBeInTheDocument()
  })

  it('evita doble envío del mismo intento lógico', async () => {
    let resolve!: (value: api.SolicitudReserva) => void
    vi.mocked(api.crearSolicitud).mockReturnValue(new Promise((resolver) => { resolve = resolver }))
    renderForm(); await completar(); fireEvent.click(screen.getByRole('button', { name: 'Crear solicitud' })); fireEvent.click(screen.getByRole('button', { name: 'Enviando...' }))
    expect(api.crearSolicitud).toHaveBeenCalledTimes(1)
    resolve(solicitud); await waitFor(() => expect(screen.getByText('Detalle creado')).toBeInTheDocument())
  })
})
