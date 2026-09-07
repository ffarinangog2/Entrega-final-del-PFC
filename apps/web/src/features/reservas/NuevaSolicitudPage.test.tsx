import { fireEvent, render, screen } from '@testing-library/react'
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
const docente = { id: 'doc-1', perfilId: 'perfil-1', codigoDocente: 'DOC-01', nombres: 'Carlos', apellidos: 'Andrade', activo: true }
const materia = { id: 'mat-1', carreraId: 'c1', codigo: 'MAT-1', nombre: 'Redes I', numeroHoras: 40, activo: true }
const periodo = { id: 'per-1', codigo: '2026-A', nombre: 'Primer período', fechaInicio: '', fechaFin: '', estado: 'ACTIVO' as const }
const horario = { id: 'h1', materiaId: 'mat-1', periodoLectivoId: 'per-1', laboratorioId: 'lab-1', docenteId: 'doc-1', diaSemana: 'LUNES', horaInicio: '08:00', horaFin: '10:00', paralelo: 'A', activo: true }

describe('NuevaSolicitudPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(academicoApi.obtenerLaboratorios).mockResolvedValue([lab])
    vi.mocked(academicoApi.obtenerMaterias).mockResolvedValue([materia])
    vi.mocked(academicoApi.obtenerPeriodoActual).mockResolvedValue(periodo)
    vi.mocked(academicoApi.obtenerDocentePorPerfil).mockResolvedValue(docente)
    vi.mocked(academicoApi.obtenerDocentes).mockResolvedValue([docente])
    vi.mocked(academicoApi.obtenerHorariosDocente).mockResolvedValue([horario])
  })

  function renderForm() {
    return render(
      <MemoryRouter initialEntries={['/reservas/nueva']}>
        <Routes>
          <Route path="/reservas/nueva" element={<NuevaSolicitudPage />} />
          <Route path="/solicitudes/:id" element={<div>Detalle Solicitud</div>} />
        </Routes>
      </MemoryRouter>,
    )
  }

  async function completar() {
    expect(await screen.findByRole('option', { name: 'MAT-1 — Redes I' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'LAB-1 — Redes' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Carlos Andrade (DOC-01)' })).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Materia'), { target: { value: 'mat-1' } })
    fireEvent.change(screen.getByLabelText('Laboratorio'), { target: { value: 'lab-1' } })
    fireEvent.change(screen.getByLabelText('Fecha'), { target: { value: '2099-08-20' } })
    fireEvent.change(screen.getByLabelText('Hora inicio'), { target: { value: '08:00' } })
    fireEvent.change(screen.getByLabelText('Hora fin'), { target: { value: '10:00' } })
    fireEvent.change(screen.getByLabelText('Motivo'), { target: { value: 'Clase práctica' } })
  }

  it('resuelve docente y carga selectores humanos sin inputs UUID manuales mostrando nombre humano', async () => {
    renderForm()
    expect(await screen.findByRole('option', { name: 'MAT-1 — Redes I' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'LAB-1 — Redes' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Carlos Andrade (DOC-01)' })).toBeInTheDocument()
    expect(screen.getByDisplayValue('2026-A — Primer período')).toBeInTheDocument()
    expect(screen.queryByPlaceholderText(/UUID/i)).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/UUID/i)).not.toBeInTheDocument()
  })

  it('envía IDs resueltos con Idempotency-Key y navega al detalle', async () => {
    vi.mocked(api.crearSolicitud).mockResolvedValue({
      id: 'sol-1',
      laboratorioId: 'lab-1',
      docenteId: 'doc-1',
      solicitanteId: 'perfil-1',
      materiaId: 'mat-1',
      periodoLectivoId: 'per-1',
      fechaReserva: '2099-08-20',
      horaInicio: '08:00',
      horaFin: '10:00',
      numeroParticipantes: 1,
      motivo: 'Clase práctica',
      observacion: '',
      estado: 'PENDIENTE',
      propuestaFecha: null,
      propuestaHoraInicio: null,
      propuestaHoraFin: null,
      propuestaLaboratorioId: null,
      propuestaObservacion: null,
      reservaId: null,
      creadaEn: '',
      actualizadaEn: '',
      version: 0,
    })

    renderForm()
    await completar()
    fireEvent.click(screen.getByRole('button', { name: 'Crear solicitud' }))

    expect(await screen.findByText('Detalle Solicitud')).toBeInTheDocument()
    expect(api.crearSolicitud).toHaveBeenCalledWith(
      expect.objectContaining({
        laboratorioId: 'lab-1',
        docenteId: 'doc-1',
        materiaId: 'mat-1',
        solicitanteId: 'perfil-1',
        fechaReserva: '2099-08-20',
        horaInicio: '08:00',
        horaFin: '10:00',
        motivo: 'Clase práctica',
      }),
      expect.stringMatching(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i),
    )
  })

  it('comprueba disponibilidad con una explicación humana', async () => {
    vi.mocked(api.consultarDisponibilidad).mockResolvedValue({
      laboratorioId: 'lab-1',
      fecha: '2099-08-20',
      horaInicio: '08:00',
      horaFin: '10:00',
      disponible: false,
      motivo: 'Existe una reserva',
    })
    renderForm()
    await completar()
    fireEvent.click(screen.getByRole('button', { name: 'Comprobar disponibilidad' }))
    expect(await screen.findByText('No disponible: Existe una reserva')).toBeInTheDocument()
  })

  it('evita doble envío del mismo intento lógico', async () => {
    vi.mocked(api.crearSolicitud).mockReturnValue(new Promise(() => {}))
    renderForm()
    await completar()
    fireEvent.click(screen.getByRole('button', { name: 'Crear solicitud' }))
    fireEvent.click(screen.getByRole('button', { name: 'Enviando...' }))
    expect(api.crearSolicitud).toHaveBeenCalledTimes(1)
  })
})
