import { fireEvent, render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AcademicPeriodSelector } from './AcademicPeriodSelector'
import {
  AcademicPeriodContext,
  type AcademicPeriodContextValue,
} from '../academicPeriodContext'
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
  fechaInicio: '2026-07-01',
  fechaFin: '2026-12-31',
  estado: 'PLANIFICADO',
  ppaNombre: 'REGULAR - 2026-2027 PPA',
  cicloAcademico: 2,
}

const mockPeriodo3: PeriodoLectivo = {
  id: 'p-3',
  codigo: '2025-2',
  nombre: '2025 CII',
  fechaInicio: '2025-07-01',
  fechaFin: '2025-12-31',
  estado: 'FINALIZADO',
  ppaNombre: 'REGULAR - 2025-2026 PPA',
  cicloAcademico: 2,
}

function renderSelector(custom: Partial<AcademicPeriodContextValue> = {}) {
  const seleccionarPeriodo = vi.fn()
  const value: AcademicPeriodContextValue = {
    periodos: [mockPeriodo1, mockPeriodo2, mockPeriodo3],
    periodoVigente: mockPeriodo1,
    periodoSeleccionado: mockPeriodo1,
    seleccionarPeriodo,
    cargando: false,
    ...custom,
  }
  const renderResult = render(
    <AcademicPeriodContext.Provider value={value}>
      <AcademicPeriodSelector />
    </AcademicPeriodContext.Provider>,
  )
  return { ...renderResult, seleccionarPeriodo }
}

describe('AcademicPeriodSelector', () => {
  it('1. con 1 período el botón abre el panel', () => {
    renderSelector({
      periodos: [mockPeriodo1],
      periodoVigente: mockPeriodo1,
      periodoSeleccionado: mockPeriodo1,
    })

    const trigger = screen.getByRole('button', { name: /Período académico:/ })
    expect(trigger).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    fireEvent.click(trigger)

    expect(screen.getByRole('dialog', { name: 'Período académico' })).toBeInTheDocument()
  })

  it('2. muestra encabezado "PERÍODO ACADÉMICO"', () => {
    renderSelector()
    fireEvent.click(screen.getByRole('button', { name: /Período académico:/ }))

    expect(screen.getByRole('heading', { name: 'PERÍODO ACADÉMICO' })).toBeInTheDocument()
  })

  it('3. el período actual aparece seleccionado', () => {
    renderSelector({
      periodos: [mockPeriodo1, mockPeriodo2],
      periodoVigente: mockPeriodo1,
      periodoSeleccionado: mockPeriodo1,
    })
    fireEvent.click(screen.getByRole('button', { name: /Período académico:/ }))

    const opcion1 = screen.getByRole('option', { name: /REGULAR 2026-2027 PPA/ })
    expect(opcion1).toHaveAttribute('aria-selected', 'true')
    expect(opcion1).toHaveClass('is-selected')
    expect(screen.getByLabelText('Seleccionado')).toBeInTheDocument()
  })

  it('4. con 0 períodos abre y muestra: "No hay períodos académicos disponibles"', () => {
    renderSelector({
      periodos: [],
      periodoVigente: null,
      periodoSeleccionado: null,
    })

    const trigger = screen.getByRole('button', { name: /Período académico:/ })
    fireEvent.click(trigger)

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText('No hay períodos académicos disponibles')).toBeInTheDocument()
  })

  it('5. con varios períodos lista todos', () => {
    renderSelector({
      periodos: [mockPeriodo1, mockPeriodo2, mockPeriodo3],
    })
    fireEvent.click(screen.getByRole('button', { name: /Período académico:/ }))

    const dialog = screen.getByRole('dialog')
    expect(within(dialog).getByText('REGULAR 2026-2027 PPA')).toBeInTheDocument()
    expect(within(dialog).getByText('REGULAR 2026-2027 SPA')).toBeInTheDocument()
    expect(within(dialog).getByText('REGULAR 2025-2026 SPA')).toBeInTheDocument()
    expect(within(dialog).getAllByRole('option')).toHaveLength(3)
  })

  it('6. seleccionar otro actualiza el contexto', () => {
    const { seleccionarPeriodo } = renderSelector()
    fireEvent.click(screen.getByRole('button', { name: /Período académico:/ }))

    const opcion2 = screen.getByRole('option', { name: /REGULAR 2026-2027 SPA/ })
    fireEvent.click(opcion2)

    expect(seleccionarPeriodo).toHaveBeenCalledWith('p-2')
  })

  it('7. seleccionar cierra el panel', () => {
    renderSelector()
    fireEvent.click(screen.getByRole('button', { name: /Período académico:/ }))
    expect(screen.getByRole('dialog')).toBeInTheDocument()

    const opcion2 = screen.getByRole('option', { name: /REGULAR 2026-2027 SPA/ })
    fireEvent.click(opcion2)

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('8. botón cerrar X funciona', () => {
    renderSelector()
    fireEvent.click(screen.getByRole('button', { name: /Período académico:/ }))
    expect(screen.getByRole('dialog')).toBeInTheDocument()

    const closeBtn = screen.getByRole('button', { name: 'Cerrar' })
    fireEvent.click(closeBtn)

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('9. no intenta persistir cambios en backend', () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    renderSelector()

    // Abrir, buscar y seleccionar
    fireEvent.click(screen.getByRole('button', { name: /Período académico:/ }))
    const input = screen.getByPlaceholderText('Buscar período...')
    fireEvent.change(input, { target: { value: '2026' } })
    const opcion1 = screen.getByRole('option', { name: /REGULAR 2026-2027 PPA/ })
    fireEvent.click(opcion1)

    expect(fetchSpy).not.toHaveBeenCalled()
    fetchSpy.mockRestore()
  })

  it('10. el período activo inicial proviene del contrato existente', () => {
    renderSelector({
      periodoSeleccionado: mockPeriodo1,
      periodoVigente: mockPeriodo1,
    })

    const trigger = screen.getByRole('button', { name: /Período académico: REGULAR 2026-2027 PPA/ })
    expect(trigger).toBeInTheDocument()
    expect(screen.getByText('REGULAR 2026-2027 PPA')).toBeInTheDocument()
  })

  it('11. búsqueda, si existe, filtra correctamente', () => {
    renderSelector({
      periodos: [mockPeriodo1, mockPeriodo2, mockPeriodo3],
    })
    fireEvent.click(screen.getByRole('button', { name: /Período académico:/ }))

    const dialog = screen.getByRole('dialog')
    const input = screen.getByPlaceholderText('Buscar período...')
    fireEvent.change(input, { target: { value: '2025' } })

    // Solo mockPeriodo3 (2025) debe estar visible dentro del diálogo
    expect(within(dialog).getByText('REGULAR 2025-2026 SPA')).toBeInTheDocument()
    expect(within(dialog).queryByText('REGULAR 2026-2027 PPA')).not.toBeInTheDocument()
    expect(within(dialog).queryByText('REGULAR 2026-2027 SPA')).not.toBeInTheDocument()

    // Búsqueda sin coincidencias
    fireEvent.change(input, { target: { value: 'inexistente' } })
    expect(screen.getByText('No se encontraron períodos para la búsqueda')).toBeInTheDocument()
  })

  it('12. selección no inventa períodos', () => {
    const { seleccionarPeriodo } = renderSelector({
      periodos: [mockPeriodo1],
    })
    fireEvent.click(screen.getByRole('button', { name: /Período académico:/ }))

    // Solo existe la opción real mockPeriodo1
    const opciones = screen.getAllByRole('option')
    expect(opciones).toHaveLength(1)

    fireEvent.click(opciones[0])
    expect(seleccionarPeriodo).toHaveBeenCalledWith('p-1')
    expect(seleccionarPeriodo).not.toHaveBeenCalledWith(expect.stringMatching(/otro|ficticio/))
  })
})
