import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as usuariosApi from '../services/usuariosApi'
import { UsuariosApiError } from '../services/usuariosApi'
import { UsuariosPage } from './UsuariosPage'
import type { Perfil } from '../services/usuariosApi'

vi.mock('../services/usuariosApi', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/usuariosApi')>()),
  listarPerfiles: vi.fn(),
  crearPerfil: vi.fn(),
}))
vi.mock('../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

const perfilMock: Perfil = {
  id: '11111111-1111-1111-1111-111111111111',
  identificacion: '0102030405',
  nombres: 'Ana',
  apellidos: 'Gómez',
  emailInstitucional: 'ana.gomez@uteq.edu.ec',
  emailPersonal: 'ana.gomez@gmail.com',
  telefono: '0999999999',
  direccion: 'Av. Principal 123',
  fechaNacimiento: '1995-05-20',
  fotoUrl: null,
  activo: true,
  creadoEn: '2026-08-18T10:00:00Z',
  actualizadoEn: '2026-08-18T10:00:00Z',
}

function renderPage() {
  render(
    <MemoryRouter>
      <UsuariosPage />
    </MemoryRouter>,
  )
}

describe('UsuariosPage', () => {
  beforeEach(() => vi.resetAllMocks())

  it('muestra loading mientras consulta el listado', () => {
    vi.mocked(usuariosApi.listarPerfiles).mockReturnValue(new Promise(() => {}))
    renderPage()
    expect(screen.getByText('Cargando perfiles...')).toBeInTheDocument()
  })

  it('renderiza la tabla con los perfiles reales', async () => {
    vi.mocked(usuariosApi.listarPerfiles).mockResolvedValue([perfilMock])
    renderPage()

    expect(await screen.findByText('Ana')).toBeInTheDocument()
    expect(screen.getByText('Gómez')).toBeInTheDocument()
    expect(screen.getByText('ana.gomez@uteq.edu.ec')).toBeInTheDocument()
    expect(screen.getByText('Sí')).toBeInTheDocument()
  })

  it('crea un perfil exitosamente y muestra el aviso de éxito', async () => {
    const user = userEvent.setup()
    vi.mocked(usuariosApi.listarPerfiles).mockResolvedValue([])
    vi.mocked(usuariosApi.crearPerfil).mockResolvedValue(perfilMock)
    renderPage()

    await screen.findByText('No hay perfiles registrados.')

    await user.type(screen.getByLabelText('Nombres'), 'Ana')
    await user.type(screen.getByLabelText('Apellidos'), 'Gómez')
    await user.type(screen.getByLabelText('Email institucional'), 'ana.gomez@uteq.edu.ec')
    await user.click(screen.getByRole('button', { name: 'Crear perfil' }))

    expect(await screen.findByText('Perfil creado correctamente.')).toBeInTheDocument()
    expect(usuariosApi.crearPerfil).toHaveBeenCalledWith(
      expect.objectContaining({
        nombres: 'Ana',
        apellidos: 'Gómez',
        emailInstitucional: 'ana.gomez@uteq.edu.ec',
      }),
    )
    expect(await screen.findByText('Ana')).toBeInTheDocument()
  })

  it('muestra el error de validación al crear', async () => {
    const user = userEvent.setup()
    vi.mocked(usuariosApi.listarPerfiles).mockResolvedValue([])
    vi.mocked(usuariosApi.crearPerfil).mockRejectedValue(
      new UsuariosApiError(400, 'Los datos enviados no son válidos'),
    )
    renderPage()

    await screen.findByText('No hay perfiles registrados.')

    await user.type(screen.getByLabelText('Nombres'), 'Ana')
    await user.type(screen.getByLabelText('Apellidos'), 'Gómez')
    await user.type(screen.getByLabelText('Email institucional'), 'no-es-un-email')
    await user.click(screen.getByRole('button', { name: 'Crear perfil' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Los datos enviados no son válidos')
  })
})
