import { describe, expect, it } from 'vitest'
import { estadoEfectivo, etiquetaPeriodo } from './academicPeriodHelpers'
import { useAcademicPeriod } from './academicPeriodContext'
import { renderHook } from '@testing-library/react'
import type { PeriodoLectivo } from './services/academicoApi'

describe('academicPeriodHelpers', () => {
  it('etiquetaPeriodo formatea correctamente para ciclo 1, 2 y por defecto', () => {
    const p1: PeriodoLectivo = {
      id: '1',
      codigo: '2026-1',
      nombre: '2026 CI',
      fechaInicio: '2026-01-01',
      fechaFin: '2026-06-30',
      estado: 'ACTIVO',
      ppaNombre: 'Periodo 2026-1 PPA',
      cicloAcademico: 1,
    }
    expect(etiquetaPeriodo(p1)).toBe('Periodo 2026-1 PPA')

    const p2: PeriodoLectivo = {
      ...p1,
      id: '2',
      ppaNombre: 'Periodo 2026-2',
      cicloAcademico: 2,
    }
    expect(etiquetaPeriodo(p2)).toBe('Periodo 2026-2 SPA')

    const p3: PeriodoLectivo = {
      ...p1,
      id: '3',
      nombre: 'Periodo Especial',
      cicloAcademico: null,
    }
    expect(etiquetaPeriodo(p3)).toBe('Periodo Especial')
  })

  it('estadoEfectivo determina ACTUAL, PLANIFICADO o FINALIZADO', () => {
    const base: PeriodoLectivo = {
      id: '1',
      codigo: '2026-1',
      nombre: '2026 CI',
      fechaInicio: '2099-01-01',
      fechaFin: '2099-06-30',
      estado: 'PLANIFICADO',
    }
    expect(estadoEfectivo(base, '1')).toBe('ACTUAL')
    expect(estadoEfectivo(base, '2')).toBe('PLANIFICADO')

    const pasado: PeriodoLectivo = {
      ...base,
      id: '3',
      fechaInicio: '2020-01-01',
    }
    expect(estadoEfectivo(pasado, '2')).toBe('FINALIZADO')
  })

  it('useAcademicPeriod devuelve valores por defecto cuando no hay contexto', () => {
    const { result } = renderHook(() => useAcademicPeriod())
    expect(result.current.periodos).toEqual([])
    expect(result.current.periodoVigente).toBeNull()
    expect(result.current.periodoSeleccionado).toBeNull()
    expect(result.current.cargando).toBe(false)
    expect(() => result.current.seleccionarPeriodo('1')).not.toThrow()
  })
})
