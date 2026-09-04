import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as academico from '../../services/academicoApi'
import * as operational from '../../services/operationalApi'
import * as usuarios from '../../services/usuariosApi'
import { StudentHome } from './StudentHome'

vi.mock('../../services/academicoApi')
vi.mock('../../services/operationalApi')
vi.mock('../../services/usuariosApi')

describe('inicio del estudiante', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(usuarios.obtenerMiContextoAcademico).mockResolvedValue({ id: 'ctx', estudianteId: 'e', carreraId: 'c', periodoId: 'p', nivel: 8, activo: true, creadoEn: '' })
    vi.mocked(academico.obtenerCarreras).mockResolvedValue([])
    vi.mocked(academico.obtenerPeriodos).mockResolvedValue([])
    vi.mocked(academico.obtenerMaterias).mockResolvedValue([])
    vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([])
    vi.mocked(academico.obtenerPisos).mockResolvedValue([])
    vi.mocked(operational.listarSesionesAbiertas).mockResolvedValue([])
    vi.mocked(operational.obtenerMiHorario).mockResolvedValue([])
  })

  it('presenta un estado vacío humano sin inventar actividad', async () => {
    render(<MemoryRouter><StudentHome /></MemoryRouter>)
    expect(await screen.findByText('Carrera institucional')).toBeInTheDocument()
    expect(screen.getByText('No existe un horario aprobado para tu contexto actual.')).toBeInTheDocument()
    expect(screen.queryByText(/Asistencia habilitada/)).not.toBeInTheDocument()
  })

  it('presenta el error controlado del contexto', async () => {
    vi.mocked(usuarios.obtenerMiContextoAcademico).mockRejectedValue(new Error('Contexto académico no disponible'))
    render(<MemoryRouter><StudentHome /></MemoryRouter>)
    expect(await screen.findByRole('alert')).toHaveTextContent('Contexto académico no disponible')
  })
})
