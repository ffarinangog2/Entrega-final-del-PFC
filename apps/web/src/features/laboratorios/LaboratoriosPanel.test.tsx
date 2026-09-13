import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { LaboratoriosPanel } from './LaboratoriosPanel'
import * as academicoApi from '../../services/academicoApi'
import type { Laboratorio } from '../../services/academicoApi'

vi.mock('../../services/academicoApi')

const laboratoriosMock: Laboratorio[] = [
  {
    id: '1',
    pisoId: 'piso-1',
    codigo: 'LAB-101',
    nombre: 'Laboratorio de Redes',
    capacidad: 30,
    descripcion: 'Laboratorio de redes',
    estado: 'DISPONIBLE',
    activo: true,
    creadoEn: '2026-01-01T00:00:00Z',
    actualizadoEn: '2026-01-01T00:00:00Z',
  },
  {
    id: '2',
    pisoId: 'piso-1',
    codigo: 'LAB-102',
    nombre: 'Laboratorio de Programacion',
    capacidad: 25,
    descripcion: 'Laboratorio de software',
    estado: 'OCUPADO',
    activo: true,
    creadoEn: '2026-01-01T00:00:00Z',
    actualizadoEn: '2026-01-01T00:00:00Z',
  },
]

describe('LaboratoriosPanel', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('muestra el estado de carga mientras llegan los datos', () => {
    vi.mocked(academicoApi.obtenerLaboratorios).mockReturnValue(new Promise(() => {}))

    render(<LaboratoriosPanel />)

    expect(screen.getByText('Cargando laboratorios...')).toBeInTheDocument()
  })

  it('muestra las tarjetas de laboratorios cuando la carga es exitosa', async () => {
    vi.mocked(academicoApi.obtenerLaboratorios).mockResolvedValue(laboratoriosMock)

    render(<LaboratoriosPanel />)

    expect(await screen.findByText('Laboratorio de Redes')).toBeInTheDocument()
    expect(screen.getByText('Laboratorio de Programacion')).toBeInTheDocument()
    expect(screen.getByText('LAB-101')).toBeInTheDocument()
    expect(screen.getByText('1 disponibles')).toBeInTheDocument()
    expect(screen.getByText('2 en total')).toBeInTheDocument()
  })

  it('muestra un mensaje de error cuando la peticion falla', async () => {
    vi.mocked(academicoApi.obtenerLaboratorios).mockRejectedValue(
      new Error('Error al obtener laboratorios: 500')
    )

    render(<LaboratoriosPanel />)

    expect(
      await screen.findByText(/No se pudo conectar con el servicio academico/)
    ).toBeInTheDocument()
  })

  it('muestra un mensaje cuando no hay laboratorios registrados', async () => {
    vi.mocked(academicoApi.obtenerLaboratorios).mockResolvedValue([])

    render(<LaboratoriosPanel />)

    expect(
      await screen.findByText('No hay laboratorios registrados todavia.')
    ).toBeInTheDocument()
  })
})