import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from './reservasApi'
import * as academicoApi from '../../services/academicoApi'
import { NuevaSolicitudPage } from './NuevaSolicitudPage'

vi.mock('./reservasApi', async (original) => ({ ...(await original<typeof import('./reservasApi')>()), crearSolicitud: vi.fn(), consultarDisponibilidad: vi.fn() }))
vi.mock('../../services/academicoApi')
vi.mock('../../auth', () => ({ useAuth: () => ({ usuario: { perfilId: 'perfil-1' } }) }))
vi.mock('../../components/DashboardLayout', () => ({ DashboardLayout: ({ children }: { children: React.ReactNode }) => <>{children}</> }))

function renderForm() { render(<MemoryRouter><NuevaSolicitudPage /></MemoryRouter>) }
async function completar() {
  fireEvent.change(await screen.findByLabelText('Laboratorio'), { target: { value: 'lab-1' } }); fireEvent.change(screen.getByLabelText('Docente ID'), { target: { value: 'doc-1' } }); fireEvent.change(screen.getByLabelText('Materia ID'), { target: { value: 'mat-1' } }); fireEvent.change(screen.getByLabelText('Periodo lectivo ID'), { target: { value: 'per-1' } }); fireEvent.change(screen.getByLabelText('Fecha'), { target: { value: '2099-08-20' } }); fireEvent.change(screen.getByLabelText('Hora inicio'), { target: { value: '08:00' } }); fireEvent.change(screen.getByLabelText('Hora fin'), { target: { value: '10:00' } }); fireEvent.change(screen.getByLabelText('Motivo'), { target: { value: 'Clase práctica' } })
}
describe('NuevaSolicitudPage', () => {
  beforeEach(() => { vi.resetAllMocks(); vi.mocked(academicoApi.obtenerLaboratorios).mockResolvedValue([{ id: 'lab-1', pisoId: 'p1', codigo: 'LAB-1', nombre: 'Redes', capacidad: 20, descripcion: '', estado: 'DISPONIBLE', activo: true, creadoEn: '', actualizadoEn: '' }]) })
  it('renderiza el formulario y reutiliza laboratorios', async () => { renderForm(); expect(screen.getByRole('heading', { name: 'Nueva solicitud' })).toBeInTheDocument(); expect(await screen.findByRole('option', { name: /LAB-1/ })).toBeInTheDocument() })
  it('envía exitosamente con Idempotency-Key y evita doble envío', async () => { let resolver: (value: api.SolicitudReserva) => void = () => {}; vi.mocked(api.crearSolicitud).mockReturnValue(new Promise((resolve) => { resolver = resolve })); renderForm(); await completar(); fireEvent.click(screen.getByRole('button', { name: 'Crear solicitud' })); expect(screen.getByRole('button', { name: 'Enviando...' })).toBeDisabled(); fireEvent.click(screen.getByRole('button', { name: 'Enviando...' })); expect(api.crearSolicitud).toHaveBeenCalledTimes(1); const key = vi.mocked(api.crearSolicitud).mock.calls[0][1]; expect(key).toBeTruthy(); resolver({} as api.SolicitudReserva); await waitFor(() => expect(screen.getByText(/creada correctamente/)).toBeInTheDocument()) })
  it('muestra error de envío', async () => { vi.mocked(api.crearSolicitud).mockRejectedValue(new Error('Solicitud inválida')); renderForm(); await completar(); fireEvent.click(screen.getByRole('button', { name: 'Crear solicitud' })); expect(await screen.findByRole('alert')).toHaveTextContent('Solicitud inválida') })
  it('muestra disponibilidad positiva', async () => { vi.mocked(api.consultarDisponibilidad).mockResolvedValue({ laboratorioId: 'lab-1', fecha: '2099-08-20', horaInicio: '08:00', horaFin: '10:00', disponible: true, motivo: null }); renderForm(); await completar(); fireEvent.click(screen.getByRole('button', { name: 'Comprobar disponibilidad' })); expect(await screen.findByText('Horario disponible.')).toBeInTheDocument() })
  it('muestra conflicto de disponibilidad', async () => { vi.mocked(api.consultarDisponibilidad).mockResolvedValue({ laboratorioId: 'lab-1', fecha: '2099-08-20', horaInicio: '08:00', horaFin: '10:00', disponible: false, motivo: 'Existe una reserva' }); renderForm(); await completar(); fireEvent.click(screen.getByRole('button', { name: 'Comprobar disponibilidad' })); expect(await screen.findByText(/Existe una reserva/)).toBeInTheDocument() })
  it('muestra error de disponibilidad', async () => { vi.mocked(api.consultarDisponibilidad).mockRejectedValue(new Error('Consulta fallida')); renderForm(); await completar(); fireEvent.click(screen.getByRole('button', { name: 'Comprobar disponibilidad' })); expect(await screen.findByRole('alert')).toHaveTextContent('Consulta fallida') })
})
