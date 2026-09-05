import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AcademicPeriodSelector } from './AcademicPeriodSelector'
import { AcademicPeriodContext, type AcademicPeriodContextValue } from '../academicPeriodContext'
import type { PeriodoLectivo } from '../services/academicoApi'

const mockPeriodo1: PeriodoLectivo = {
  id: 'p-1',
  codigo: '2026-1',
  nombre: '2026 CI',
  fechaInicio: '2026-01-01',
  fechaFin: '2026-06-30',
  estado: 'ACTIVO',
  ppaNombre: 'Periodo 2026-1 PPA',
  cicloAcademico: 1,
}

const mockPeriodo2: PeriodoLectivo = {
  id: 'p-2',
  codigo: '2026-2',
  nombre: '2026 CII',
  fechaInicio: '2099-07-01',
  fechaFin: '2099-12-31',
  estado: 'PLANIFICADO',
  ppaNombre: 'Periodo 2026-2',
  cicloAcademico: 2,
}

function renderSelector(custom: Partial<AcademicPeriodContextValue> = {}) {
  const seleccionarPeriodo = vi.fn()
  const value: AcademicPeriodContextValue = {
    periodos: [mockPeriodo1, mockPeriodo2],
    periodoVigente: mockPeriodo1,
    periodoSeleccionado: mockPeriodo1,
    seleccionarPeriodo,
    cargando: false,
    ...custom,
  }
  const renderResult = render(
    <AcademicPeriodContext.Provider value={value}>
      <AcademicPeriodSelector />
    </AcademicPeriodContext.Provider>
  )
  return { ...renderResult, seleccionarPeriodo }
}

describe('AcademicPeriodSelector', () => {
  it('muestra estado de carga cuando cargando es true', () => {
    renderSelector({ cargando: true })
    expect(screen.getByText('Cargando período…')).toBeInTheDocument()
  })

  it('muestra texto por defecto si no hay periodoSeleccionado', () => {
    renderSelector({ periodoSeleccionado: null })
    expect(screen.getByText('Período académico')).toBeInTheDocument()
  })

  it('muestra el periodo seleccionado y lista opciones con filtro de busqueda', () => {
    const { seleccionarPeriodo } = renderSelector()
    expect(screen.getByText('Periodo 2026-1 PPA')).toBeInTheDocument()

    const input = screen.getByLabelText('Buscar período')
    fireEvent.change(input, { target: { value: 'SPA' } })

    expect(screen.queryByRole('button', { name: /Periodo 2026-1 PPA/ })).not.toBeInTheDocument()
    const opcion2 = screen.getByRole('button', { name: /Periodo 2026-2 SPA/ })
    expect(opcion2).toBeInTheDocument()

    fireEvent.click(opcion2)
    expect(seleccionarPeriodo).toHaveBeenCalledWith('p-2')
  })
})
