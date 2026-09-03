import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as academico from '../../services/academicoApi'
import * as usuarios from '../../services/usuariosApi'
import { AdminInstitutionPage } from './AdminInstitutionPage'

vi.mock('../../services/academicoApi')
vi.mock('../../services/usuariosApi')
vi.mock('../../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

describe('AdminInstitutionPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([])
    vi.mocked(academico.obtenerPisos).mockResolvedValue([
      { id: 'piso-1', bloqueId: 'b1', numero: 1, descripcion: '', activo: true },
      { id: 'piso-2', bloqueId: 'b1', numero: 2, descripcion: '', activo: true },
    ])
    vi.mocked(academico.obtenerEquipos).mockResolvedValue([])
    vi.mocked(academico.obtenerTiposEquipo).mockResolvedValue([])
    vi.mocked(academico.obtenerCampus).mockResolvedValue([])
    vi.mocked(academico.obtenerCarreras).mockResolvedValue([])
    vi.mocked(academico.obtenerMaterias).mockResolvedValue([])
    vi.mocked(academico.obtenerPeriodos).mockResolvedValue([])
    vi.mocked(usuarios.listarAdministradores).mockResolvedValue([
      { id: 'admin-1', perfilId: 'perfil-1', codigoAdministrador: 'adminpiso.01', cargo: 'Administrador de piso', pisoId: 'piso-1', activo: true },
    ])
    vi.mocked(usuarios.listarPerfiles).mockResolvedValue([
      { id: 'perfil-1', identificacion: '1', nombres: 'Ana', apellidos: 'Piso', emailInstitucional: 'ana@scli.edu.ec', emailPersonal: null, telefono: null, direccion: null, fechaNacimiento: null, fotoUrl: null, activo: true, creadoEn: '', actualizadoEn: '' },
    ])
  })

  it('muestra la asociación institucional entre administrador y piso', async () => {
    render(<AdminInstitutionPage />)
    expect(await screen.findByText('adminpiso.01')).toBeInTheDocument()
    expect(screen.getByText('Ana Piso')).toBeInTheDocument()
    expect(screen.getByLabelText('Piso de adminpiso.01')).toHaveValue('piso-1')
  })

  it('crea un laboratorio usando un piso real y sin pedir UUID', async () => {
    vi.mocked(academico.crearLaboratorio).mockResolvedValue({
      id: 'lab-9', pisoId: 'piso-1', codigo: 'LAB-09', nombre: 'Inteligencia Artificial',
      capacidad: 30, descripcion: '', estado: 'DISPONIBLE', activo: true, creadoEn: '', actualizadoEn: '',
    })
    render(<AdminInstitutionPage />)
    await screen.findByText('adminpiso.01')
    fireEvent.change(screen.getByLabelText('Código'), { target: { value: 'LAB-09' } })
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'Inteligencia Artificial' } })
    fireEvent.change(screen.getByLabelText('Piso'), { target: { value: 'piso-1' } })
    fireEvent.change(screen.getByLabelText('Capacidad'), { target: { value: '30' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear laboratorio' }))
    await waitFor(() => expect(academico.crearLaboratorio).toHaveBeenCalledWith(expect.objectContaining({
      pisoId: 'piso-1', codigo: 'LAB-09', capacidad: 30,
    })))
    expect(screen.queryByLabelText(/UUID/i)).not.toBeInTheDocument()
  })

  it('actualiza la fuente única de asignación de piso en Usuarios', async () => {
    vi.mocked(usuarios.actualizarAdministrador).mockResolvedValue({
      id: 'admin-1', perfilId: 'perfil-1', codigoAdministrador: 'adminpiso.01', cargo: 'Administrador de piso', pisoId: 'piso-2', activo: true,
    })
    render(<AdminInstitutionPage />)
    fireEvent.change(await screen.findByLabelText('Piso de adminpiso.01'), { target: { value: 'piso-2' } })
    await waitFor(() => expect(usuarios.actualizarAdministrador).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'admin-1' }), 'piso-2',
    ))
  })

  it('edita y desactiva un laboratorio existente', async () => {
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([{
      id: 'lab-1', pisoId: 'piso-1', codigo: 'LAB-01', nombre: 'Software', capacidad: 20,
      descripcion: 'Principal', estado: 'DISPONIBLE', activo: true, creadoEn: '', actualizadoEn: '',
    }])
    vi.mocked(academico.actualizarLaboratorio).mockResolvedValue({
      id: 'lab-1', pisoId: 'piso-2', codigo: 'LAB-01', nombre: 'Software II', capacidad: 25,
      descripcion: 'Principal', estado: 'DISPONIBLE', activo: true, creadoEn: '', actualizadoEn: '',
    })
    vi.mocked(academico.cambiarEstadoLaboratorio).mockResolvedValue({
      id: 'lab-1', pisoId: 'piso-1', codigo: 'LAB-01', nombre: 'Software', capacidad: 20,
      descripcion: 'Principal', estado: 'INACTIVO', activo: true, creadoEn: '', actualizadoEn: '',
    })
    render(<AdminInstitutionPage />)
    fireEvent.click(await screen.findByRole('button', { name: 'Editar' }))
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'Software II' } })
    fireEvent.change(screen.getByLabelText('Piso'), { target: { value: 'piso-2' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    await waitFor(() => expect(academico.actualizarLaboratorio).toHaveBeenCalledWith(
      'lab-1', expect.objectContaining({ pisoId: 'piso-2', nombre: 'Software II' }),
    ))
    fireEvent.click(screen.getByRole('button', { name: 'Desactivar' }))
    await waitFor(() => expect(academico.cambiarEstadoLaboratorio).toHaveBeenCalledWith('lab-1', 'INACTIVO'))
  })

  it('crea equipo relacionándolo con laboratorio y tipo existentes', async () => {
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([{
      id: 'lab-1', pisoId: 'piso-1', codigo: 'LAB-01', nombre: 'Software', capacidad: 20,
      descripcion: '', estado: 'DISPONIBLE', activo: true, creadoEn: '', actualizadoEn: '',
    }])
    vi.mocked(academico.obtenerTiposEquipo).mockResolvedValue([
      { id: 'tipo-1', nombre: 'Computador', descripcion: '', activo: true },
    ])
    vi.mocked(academico.crearEquipo).mockResolvedValue({
      id: 'eq-1', laboratorioId: 'lab-1', tipoEquipoId: 'tipo-1', codigoInventario: 'PC-01',
      numeroSerie: null, marca: null, modelo: null, estado: 'DISPONIBLE', activo: true,
    })
    render(<AdminInstitutionPage />)
    await screen.findByText('adminpiso.01')
    fireEvent.change(screen.getByLabelText('Laboratorio'), { target: { value: 'lab-1' } })
    fireEvent.change(screen.getByLabelText('Tipo'), { target: { value: 'tipo-1' } })
    fireEvent.change(screen.getByLabelText('Código de inventario'), { target: { value: 'PC-01' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear equipo' }))
    await waitFor(() => expect(academico.crearEquipo).toHaveBeenCalledWith(expect.objectContaining({
      laboratorioId: 'lab-1', tipoEquipoId: 'tipo-1', codigoInventario: 'PC-01',
    })))
  })

  it('presenta un error controlado cuando falla la carga institucional', async () => {
    vi.mocked(academico.obtenerPisos).mockRejectedValue(new Error('Servicio no disponible'))
    render(<AdminInstitutionPage />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Servicio no disponible')
    expect(screen.queryByRole('button', { name: 'Crear laboratorio' })).not.toBeInTheDocument()
  })

  it('permite cancelar edición y reactivar un laboratorio inactivo', async () => {
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([{
      id: 'lab-2', pisoId: 'piso-2', codigo: 'LAB-02', nombre: 'Redes', capacidad: 25,
      descripcion: '', estado: 'INACTIVO', activo: false, creadoEn: '', actualizadoEn: '',
    }])
    vi.mocked(academico.cambiarEstadoLaboratorio).mockResolvedValue({
      id: 'lab-2', pisoId: 'piso-2', codigo: 'LAB-02', nombre: 'Redes', capacidad: 25,
      descripcion: '', estado: 'DISPONIBLE', activo: true, creadoEn: '', actualizadoEn: '',
    })
    render(<AdminInstitutionPage />)
    fireEvent.click(await screen.findByRole('button', { name: 'Editar' }))
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(screen.queryByRole('button', { name: 'Guardar cambios' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Activar' }))
    await waitFor(() => expect(academico.cambiarEstadoLaboratorio).toHaveBeenCalledWith('lab-2', 'DISPONIBLE'))
  })

  it('muestra el conflicto devuelto al crear un laboratorio', async () => {
    vi.mocked(academico.crearLaboratorio).mockRejectedValue(new Error('El código ya existe'))
    render(<AdminInstitutionPage />)
    await screen.findByText('adminpiso.01')
    fireEvent.change(screen.getByLabelText('Código'), { target: { value: 'LAB-01' } })
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'Duplicado' } })
    fireEvent.change(screen.getByLabelText('Piso'), { target: { value: 'piso-1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear laboratorio' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('El código ya existe')
  })

  it('permite retirar una asignación y muestra errores del servicio de Usuarios', async () => {
    vi.mocked(usuarios.actualizarAdministrador).mockRejectedValue(new Error('El administrador tiene operación activa'))
    render(<AdminInstitutionPage />)
    fireEvent.change(await screen.findByLabelText('Piso de adminpiso.01'), { target: { value: '' } })
    expect(await screen.findByRole('alert')).toHaveTextContent('El administrador tiene operación activa')
    expect(usuarios.actualizarAdministrador).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'admin-1' }), null,
    )
  })
})
