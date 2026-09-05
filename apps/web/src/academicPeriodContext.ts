import { createContext, useContext } from 'react'
import type { PeriodoLectivo } from './services/academicoApi'

export type AcademicPeriodContextValue = {
  periodos: PeriodoLectivo[]
  periodoVigente: PeriodoLectivo | null
  periodoSeleccionado: PeriodoLectivo | null
  seleccionarPeriodo: (id: string) => void
  cargando: boolean
}

export const STORAGE_KEY = 'scli.selectedAcademicPeriod'

export const AcademicPeriodContext = createContext<AcademicPeriodContextValue | null>(null)

export function useAcademicPeriod() {
  const value = useContext(AcademicPeriodContext)
  return value ?? {
    periodos: [],
    periodoVigente: null,
    periodoSeleccionado: null,
    seleccionarPeriodo: () => undefined,
    cargando: false,
  }
}
