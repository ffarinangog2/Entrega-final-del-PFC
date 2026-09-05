import { render, screen } from '@testing-library/react'
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
  ppaNombre: 'REGULAR - 2026-2027 PPA',
  cicloAcademico: 1,
}

const mockPeriodo2: PeriodoLectivo = {
  id: 'p-2',
  codigo: '2026-2',
  nombre: '2026 CII',
  fechaInicio: '2099-07-01',
  fechaFin: '2099-12-31',
  estado: 'PLANIFICADO',
  ppaNombre: 'REGULAR - 2026-2027 PPA',
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

  it('muestra la ausencia de un período efectivo', () => {
    renderSelector({ periodoVigente: null, periodoSeleccionado: null })
    expect(screen.getByText('Sin período académico actual')).toBeInTheDocument()
  })

  it('muestra únicamente el PPA efectivo como indicador no editable', () => {
    const { seleccionarPeriodo } = renderSelector()
    expect(screen.getByText('REGULAR 2026-2027 PPA')).toBeInTheDocument()
    expect(screen.queryByText(/SPA/)).not.toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument()
    expect(seleccionarPeriodo).not.toHaveBeenCalled()
  })
})
