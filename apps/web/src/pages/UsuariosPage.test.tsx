import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as usuarios from '../services/usuariosApi'
import * as auth from '../services/authApi'
import * as academico from '../services/academicoApi'
import { UsuariosPage } from './UsuariosPage'

vi.mock('../services/usuariosApi')
vi.mock('../services/authApi')
vi.mock('../services/academicoApi')
vi.mock('../components/DashboardLayout', () => ({ DashboardLayout: ({ children }: { children: React.ReactNode }) => <>{children}</> }))

const perfil = { id: 'perfil-1', identificacion: '0102030405', nombres: 'Ana', apellidos: 'Gómez', emailInstitucional: 'ana@uteq.edu.ec', emailPersonal: null, telefono: null, direccion: null, fechaNacimiento: null, fotoUrl: null, activo: true, creadoEn: '', actualizadoEn: '' }
const cuenta: auth.UsuarioInstitucional = { id: 'auth-1', perfilId: 'perfil-1', username: 'ana.gomez', email: 'ana@uteq.edu.ec', rol: 'COORDINADOR', activo: true }

function renderPage() { render(<MemoryRouter><UsuariosPage /></MemoryRouter>) }

describe('UsuariosPage administrativa', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(usuarios.listarPerfiles).mockResolvedValue([perfil])
    vi.mocked(auth.listarUsuariosInstitucionales).mockResolvedValue([cuenta])
    vi.mocked(usuarios.obtenerAsociacionRol).mockResolvedValue({ pisoId: null, carreraId: 'carrera-1' })
    vi.mocked(usuarios.obtenerContextosAcademicos).mockResolvedValue([])
    vi.mocked(academico.obtenerPisos).mockResolvedValue([{ id: 'piso-2', bloqueId: 'b1', numero: 2, descripcion: '', activo: true }])
    vi.mocked(academico.obtenerCarreras).mockResolvedValue([{ id: 'carrera-1', facultadId: 'f1', codigo: 'IS', nombre: 'Ingeniería de Software', activo: true }])
    vi.mocked(academico.obtenerPeriodos).mockResolvedValue([{ id: 'periodo-1', codigo: 'C1', nombre: 'Ciclo actual', fechaInicio: '2026-05-01', fechaFin: '2026-09-18', estado: 'ACTIVO' }])
  })

  it('lista y filtra cuentas por rol y estado', async () => {
    const user = userEvent.setup(); renderPage()
    expect(await screen.findByText('ana.gomez')).toBeInTheDocument()
    await user.selectOptions(screen.getAllByLabelText('Rol')[0], 'DOCENTE')
    expect(screen.getByText('No existen usuarios para los filtros seleccionados.')).toBeInTheDocument()
  })

  it('crea perfil, asociación de piso y credenciales reales', async () => {
    const user = userEvent.setup()
    vi.mocked(usuarios.listarPerfiles).mockResolvedValue([]); vi.mocked(auth.listarUsuariosInstitucionales).mockResolvedValue([])
    vi.mocked(usuarios.crearUsuarioInstitucionalCompleto).mockResolvedValue(perfil)
    renderPage(); await screen.findByText('No existen usuarios para los filtros seleccionados.')
    await user.type(screen.getByLabelText('Identificación'), '0102030405'); await user.type(screen.getByLabelText('Nombres'), 'Ana')
    await user.type(screen.getByLabelText('Apellidos'), 'Gómez'); await user.type(screen.getByLabelText('Correo institucional'), 'ana@uteq.edu.ec')
    await user.type(screen.getByLabelText('Nombre de usuario'), 'ana.gomez'); await user.type(screen.getByLabelText('Contraseña inicial'), 'ClaveSegura1!')
    const roles = screen.getAllByLabelText('Rol')
    await user.selectOptions(roles[roles.length - 1], 'ADMINISTRADOR_PISO'); await user.selectOptions(screen.getByLabelText('Piso'), 'piso-2')
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }))
    await waitFor(() => expect(usuarios.crearUsuarioInstitucionalCompleto).toHaveBeenCalledWith(expect.objectContaining({ rol: 'ADMINISTRADOR_PISO', pisoId: 'piso-2', carreraId: null })))
  })

  it('cambia rol y asociación de carrera al editar', async () => {
    const user = userEvent.setup(); vi.mocked(usuarios.actualizarUsuarioInstitucionalCompleto).mockResolvedValue(perfil)
    renderPage(); await user.click(await screen.findByRole('button', { name: 'Editar' }))
    await waitFor(() => expect(screen.getByLabelText('Carrera')).toHaveValue('carrera-1'))
    const roles = screen.getAllByLabelText('Rol')
    await user.selectOptions(roles[roles.length - 1], 'COORDINADOR'); await user.selectOptions(screen.getByLabelText('Carrera'), 'carrera-1')
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    await waitFor(() => expect(usuarios.actualizarUsuarioInstitucionalCompleto).toHaveBeenCalledWith('perfil-1', expect.objectContaining({ rol: 'COORDINADOR', pisoId: null, carreraId: 'carrera-1' })))
  })

  it('activa o desactiva la cuenta en Auth', async () => {
    const user = userEvent.setup(); vi.mocked(usuarios.actualizarUsuarioInstitucionalCompleto).mockResolvedValue({ ...perfil, activo: false })
    renderPage(); await user.click(await screen.findByRole('button', { name: 'Desactivar' }))
    await waitFor(() => expect(usuarios.actualizarUsuarioInstitucionalCompleto).toHaveBeenCalledWith('perfil-1', expect.objectContaining({ activo: false })))
  })

  it('muestra el fallo atómico si backend rechaza la desactivación', async () => {
    const user = userEvent.setup()
    vi.mocked(usuarios.actualizarUsuarioInstitucionalCompleto).mockRejectedValue(new Error('Auth no disponible'))
    renderPage(); await user.click(await screen.findByRole('button', { name: 'Desactivar' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Auth no disponible')
  })

  it('conserva carrera, ciclo y nivel al cambiar el estado de un estudiante', async () => {
    const user = userEvent.setup()
    vi.mocked(auth.listarUsuariosInstitucionales).mockResolvedValue([{ ...cuenta, rol: 'ESTUDIANTE' }])
    vi.mocked(usuarios.obtenerContextosAcademicos).mockResolvedValue([{ id: 'ctx-1', estudianteId: 'est-1', carreraId: 'carrera-1', periodoId: 'periodo-1', nivel: 7, activo: true, creadoEn: '' }])
    vi.mocked(usuarios.actualizarUsuarioInstitucionalCompleto).mockResolvedValue({ ...perfil, activo: false })
    renderPage()
    await user.click(await screen.findByRole('button', { name: 'Desactivar' }))
    await waitFor(() => expect(usuarios.actualizarUsuarioInstitucionalCompleto).toHaveBeenCalledWith('perfil-1', expect.objectContaining({
      rol: 'ESTUDIANTE', activo: false, carreraId: 'carrera-1', periodoId: 'periodo-1', nivel: 7,
    })))
  })
})
