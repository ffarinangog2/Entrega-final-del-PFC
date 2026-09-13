import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MonitoreoPanel } from './MonitoreoPanel'
import * as academicoApi from '../../services/academicoApi'
import type { SerieEstado } from '../../services/academicoApi'

vi.mock('../../services/academicoApi')

const seriesMock: SerieEstado[] = [
  {
    estado: 'DISPONIBLE',
    puntos: [
      { instante: '2026-08-23T02:00:00Z', valor: 3 },
      { instante: '2026-08-23T02:15:00Z', valor: 2 },
    ],
  },
  {
    estado: 'OCUPADO',
    puntos: [
      { instante: '2026-08-23T02:00:00Z', valor: 1 },
      { instante: '2026-08-23T02:15:00Z', valor: 2 },
    ],
  },
]

describe('MonitoreoPanel', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('muestra el estado de carga mientras llegan los datos', () => {
    vi.mocked(academicoApi.obtenerOcupacionHistorica).mockReturnValue(new Promise(() => {}))

    render(<MonitoreoPanel />)

    expect(screen.getByText('Cargando ocupacion historica...')).toBeInTheDocument()
  })

  it('muestra el titulo y el grafico cuando la carga es exitosa', async () => {
    vi.mocked(academicoApi.obtenerOcupacionHistorica).mockResolvedValue(seriesMock)

    render(<MonitoreoPanel />)

    expect(await screen.findByText('Ocupacion en la ultima hora')).toBeInTheDocument()
  })

  it('muestra un mensaje de error cuando la peticion falla', async () => {
    vi.mocked(academicoApi.obtenerOcupacionHistorica).mockRejectedValue(
      new Error('Error al obtener ocupacion historica: 500')
    )

    render(<MonitoreoPanel />)

    expect(
      await screen.findByText(/No se pudo conectar con el servicio de metricas/)
    ).toBeInTheDocument()
  })

  it('muestra un mensaje cuando no hay historico todavia', async () => {
    vi.mocked(academicoApi.obtenerOcupacionHistorica).mockResolvedValue([])

    render(<MonitoreoPanel />)

    expect(
      await screen.findByText('Aun no hay historico suficiente para graficar.')
    ).toBeInTheDocument()
  })
})