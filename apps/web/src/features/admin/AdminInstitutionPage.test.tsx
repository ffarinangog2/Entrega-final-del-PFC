import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as academico from '../../services/academicoApi'
import * as usuarios from '../../services/usuariosApi'
import {
  AdminAsignacionesPage,
  AdminCatalogosPage,
  AdminEquiposPage,
  AdminInstitutionPage,
  AdminLaboratoriosPage,
  AdminOverviewPage,
  AdminPisosPage,
} from './AdminInstitutionPage'

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
    vi.mocked(academico.obtenerBloques).mockResolvedValue([])
    vi.mocked(academico.obtenerFacultades).mockResolvedValue([])
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

  it.each([
    [AdminOverviewPage, 'Administración institucional', [], ['Laboratorios', 'Gestión de pisos', 'Equipos', 'Estructura académica', 'Administradores de piso']],
    [AdminLaboratoriosPage, 'Gestión de laboratorios', ['Laboratorios'], ['Gestión de pisos', 'Equipos', 'Estructura académica', 'Administradores de piso']],
    [AdminPisosPage, 'Gestión de pisos', ['Gestión de pisos'], ['Laboratorios', 'Equipos', 'Estructura académica', 'Administradores de piso']],
    [AdminEquiposPage, 'Gestión de equipos', ['Equipos', 'Inventario de equipos'], ['Laboratorios', 'Gestión de pisos', 'Estructura académica', 'Administradores de piso']],
    [AdminCatalogosPage, 'Catálogos institucionales', ['Estructura académica', 'Gestionar campus', 'Gestionar carreras', 'Gestionar materias'], ['Laboratorios', 'Gestión de pisos', 'Equipos', 'Administradores de piso']],
    [AdminAsignacionesPage, 'Asignaciones administrativas', ['Administradores de piso'], ['Laboratorios', 'Gestión de pisos', 'Equipos', 'Estructura académica']],
  ])('aísla el módulo de %s', async (Page, title, headings, excludedHeadings) => {
    render(<Page />)
    expect(await screen.findByRole('heading', { name: title, level: 1 })).toBeInTheDocument()
    headings.forEach((heading) => expect(screen.getByRole('heading', { name: heading, level: 2 })).toBeInTheDocument())
    excludedHeadings.forEach((heading) => expect(screen.queryByRole('heading', { name: heading, level: 2 })).not.toBeInTheDocument())
  })

  it('crea un laboratorio usando un piso real y sin pedir UUID', async () => {
    const lab09 = {
      id: 'lab-9', pisoId: 'piso-2', codigo: 'LAB-09', nombre: 'Inteligencia Artificial',
      capacidad: 30, descripcion: '', estado: 'DISPONIBLE' as const, activo: true, creadoEn: '', actualizadoEn: '',
    }
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValueOnce([]).mockResolvedValue([lab09])
    vi.mocked(academico.crearLaboratorio).mockResolvedValue({
      id: 'lab-9', pisoId: 'piso-2', codigo: 'LAB-09', nombre: 'Inteligencia Artificial',
      capacidad: 30, descripcion: '', estado: 'DISPONIBLE', activo: true, creadoEn: '', actualizadoEn: '',
    })
    render(<AdminInstitutionPage />)
    await screen.findByText('adminpiso.01')
    const seccion = screen.getByRole('heading', { name: 'Laboratorios' }).closest('section')!
    fireEvent.change(within(seccion).getByLabelText('Código'), { target: { value: 'LAB-09' } })
    fireEvent.change(within(seccion).getByLabelText('Nombre'), { target: { value: 'Inteligencia Artificial' } })
    fireEvent.change(within(seccion).getByLabelText('Piso'), { target: { value: 'piso-2' } })
    fireEvent.change(within(seccion).getByLabelText('Capacidad'), { target: { value: '30' } })
    fireEvent.click(within(seccion).getByRole('button', { name: 'Crear laboratorio' }))
    await waitFor(() => expect(academico.crearLaboratorio).toHaveBeenCalledWith(expect.objectContaining({
      pisoId: 'piso-2', codigo: 'LAB-09', capacidad: 30,
    })))
    const piso2 = screen.getByText('Piso 2', { selector: 'strong' }).closest('article')!
    fireEvent.click(within(piso2).getByRole('button', { name: 'Ver laboratorios' }))
    expect(await within(piso2).findByText(/LAB-09/)).toBeInTheDocument()
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
    await screen.findByText('adminpiso.01')
    const seccion = screen.getByRole('heading', { name: 'Laboratorios' }).closest('section')!
    fireEvent.click(within(seccion).getByRole('button', { name: 'Editar' }))
    fireEvent.change(within(seccion).getByLabelText('Nombre'), { target: { value: 'Software II' } })
    fireEvent.change(within(seccion).getByLabelText('Piso'), { target: { value: 'piso-2' } })
    fireEvent.click(within(seccion).getByRole('button', { name: 'Guardar cambios' }))
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
    await screen.findByText('adminpiso.01')
    const seccion = screen.getByRole('heading', { name: 'Laboratorios' }).closest('section')!
    fireEvent.click(within(seccion).getByRole('button', { name: 'Editar' }))
    expect(within(seccion).getByRole('button', { name: 'Cancelar' })).toBeInTheDocument()
    fireEvent.click(within(seccion).getByRole('button', { name: 'Cancelar' }))
    expect(screen.queryByRole('button', { name: 'Guardar cambios' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Activar' }))
    await waitFor(() => expect(academico.cambiarEstadoLaboratorio).toHaveBeenCalledWith('lab-2', 'DISPONIBLE'))
  })

  it('muestra el conflicto devuelto al crear un laboratorio', async () => {
    vi.mocked(academico.crearLaboratorio).mockRejectedValue(new Error('El código ya existe'))
    render(<AdminInstitutionPage />)
    await screen.findByText('adminpiso.01')
    const seccion = screen.getByRole('heading', { name: 'Laboratorios' }).closest('section')!
    fireEvent.change(within(seccion).getByLabelText('Código'), { target: { value: 'LAB-01' } })
    fireEvent.change(within(seccion).getByLabelText('Nombre'), { target: { value: 'Duplicado' } })
    fireEvent.change(within(seccion).getByLabelText('Piso'), { target: { value: 'piso-1' } })
    fireEvent.click(within(seccion).getByRole('button', { name: 'Crear laboratorio' }))
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

  it('crea catálogos académicos conservando sus relaciones', async () => {
    vi.mocked(academico.obtenerBloques).mockResolvedValue([{ id: 'bloque-1', campusId: 'campus-1', codigo: 'B1', nombre: 'Bloque 1', activo: true }])
    vi.mocked(academico.obtenerFacultades).mockResolvedValue([{ id: 'fac-1', codigo: 'F1', nombre: 'Facultad', descripcion: '', activo: true }])
    vi.mocked(academico.obtenerCampus).mockResolvedValue([{ id: 'campus-1', codigo: 'C1', nombre: 'Central', direccion: '', activo: true }])
    vi.mocked(academico.obtenerCarreras).mockResolvedValue([{ id: 'carrera-1', facultadId: 'fac-1', codigo: 'IS', nombre: 'Software', activo: true }])
    vi.mocked(academico.crearPiso).mockResolvedValue({ id: 'piso-3', bloqueId: 'bloque-1', numero: 3, descripcion: '', activo: true })
    vi.mocked(academico.crearCampus).mockResolvedValue({ id: 'campus-2', codigo: 'C2', nombre: 'Norte', direccion: '', activo: true })
    vi.mocked(academico.crearCarrera).mockResolvedValue({ id: 'carrera-2', facultadId: 'fac-1', codigo: 'TI', nombre: 'TI', activo: true })
    vi.mocked(academico.crearMateria).mockResolvedValue({ id: 'materia-2', carreraId: 'carrera-1', codigo: 'MAT2', nombre: 'Redes', numeroHoras: 3, nivel: 3, activo: true })
    render(<AdminInstitutionPage />)
    await screen.findByText('adminpiso.01')

    const pisosSection = screen.getByRole('heading', { name: 'Gestión de pisos' }).closest('section')!
    fireEvent.change(within(pisosSection).getByLabelText('Bloque / campus'), { target: { value: 'bloque-1' } })
    fireEvent.change(within(pisosSection).getByLabelText('Número'), { target: { value: '3' } })
    fireEvent.click(within(pisosSection).getByRole('button', { name: 'Crear piso' }))
    await waitFor(() => expect(academico.crearPiso).toHaveBeenCalledWith(expect.objectContaining({ bloqueId: 'bloque-1', numero: 3 })))

    const campusSection = screen.getByRole('heading', { name: 'Gestionar campus' }).closest('section')!
    fireEvent.change(within(campusSection).getByLabelText('Código'), { target: { value: 'C2' } })
    fireEvent.change(within(campusSection).getByLabelText('Nombre'), { target: { value: 'Norte' } })
    fireEvent.click(within(campusSection).getByRole('button', { name: 'Crear campus' }))
    await waitFor(() => expect(academico.crearCampus).toHaveBeenCalled())

    const carreraSection = screen.getByRole('heading', { name: 'Gestionar carreras' }).closest('section')!
    fireEvent.change(within(carreraSection).getByLabelText('Facultad'), { target: { value: 'fac-1' } })
    fireEvent.change(within(carreraSection).getByLabelText('Código'), { target: { value: 'TI' } })
    fireEvent.change(within(carreraSection).getByLabelText('Nombre'), { target: { value: 'TI' } })
    fireEvent.click(within(carreraSection).getByRole('button', { name: 'Crear carrera' }))
    await waitFor(() => expect(academico.crearCarrera).toHaveBeenCalled())

    const materiaSection = screen.getByRole('heading', { name: 'Gestionar materias' }).closest('section')!
    fireEvent.change(within(materiaSection).getByLabelText('Carrera'), { target: { value: 'carrera-1' } })
    fireEvent.change(within(materiaSection).getByLabelText('Código'), { target: { value: 'MAT2' } })
    fireEvent.change(within(materiaSection).getByLabelText('Nombre'), { target: { value: 'Redes' } })
    fireEvent.change(within(materiaSection).getByLabelText('Nivel'), { target: { value: '3' } })
    fireEvent.click(within(materiaSection).getByRole('button', { name: 'Crear materia' }))
    await waitFor(() => expect(academico.crearMateria).toHaveBeenCalledWith(expect.objectContaining({ carreraId: 'carrera-1', nivel: 3 })))
  })
})
