import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { obtenerPeriodoActual, obtenerPeriodos, type PeriodoLectivo } from './services/academicoApi'

type AcademicPeriodContextValue = {
  periodos: PeriodoLectivo[]
  periodoVigente: PeriodoLectivo | null
  periodoSeleccionado: PeriodoLectivo | null
  seleccionarPeriodo: (id: string) => void
  cargando: boolean
}

const AcademicPeriodContext = createContext<AcademicPeriodContextValue | null>(null)
const STORAGE_KEY = 'scli.selectedAcademicPeriod'

export function etiquetaPeriodo(periodo: PeriodoLectivo) {
  if (periodo.cicloAcademico === 1) return periodo.ppaNombre?.replace(/\s*PPA\s*$/i, '').trim() + ' PPA'
  if (periodo.cicloAcademico === 2) return periodo.ppaNombre?.replace(/\s*PPA\s*$/i, '').trim() + ' SPA'
  return periodo.nombre
}

export function estadoEfectivo(periodo: PeriodoLectivo, vigenteId?: string) {
  if (periodo.id === vigenteId) return 'ACTUAL'
  const hoy = new Date().toISOString().slice(0, 10)
  return periodo.fechaInicio > hoy ? 'PLANIFICADO' : 'FINALIZADO'
}

export function AcademicPeriodProvider({ children }: { children: ReactNode }) {
  const [periodos, setPeriodos] = useState<PeriodoLectivo[]>([])
  const [periodoVigente, setPeriodoVigente] = useState<PeriodoLectivo | null>(null)
  const [seleccionadoId, setSeleccionadoId] = useState(() => sessionStorage.getItem(STORAGE_KEY) ?? '')
  const [cargando, setCargando] = useState(true)
  useEffect(() => {
    let activo = true
    Promise.all([obtenerPeriodos(), obtenerPeriodoActual()]).then(([lista, vigente]) => {
      if (!activo) return
      setPeriodos(lista.sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio)))
      setPeriodoVigente(vigente)
      setSeleccionadoId((actual) => actual && lista.some((p) => p.id === actual) ? actual : vigente.id)
    }).finally(() => { if (activo) setCargando(false) })
    return () => { activo = false }
  }, [])
  const periodoSeleccionado = useMemo(
    () => periodos.find((p) => p.id === seleccionadoId) ?? periodoVigente,
    [periodos, periodoVigente, seleccionadoId],
  )
  const seleccionarPeriodo = (id: string) => {
    sessionStorage.setItem(STORAGE_KEY, id)
    setSeleccionadoId(id)
  }
  return <AcademicPeriodContext.Provider value={{ periodos, periodoVigente, periodoSeleccionado, seleccionarPeriodo, cargando }}>{children}</AcademicPeriodContext.Provider>
}

export function useAcademicPeriod() {
  const value = useContext(AcademicPeriodContext)
  return value ?? { periodos: [], periodoVigente: null, periodoSeleccionado: null, seleccionarPeriodo: () => undefined, cargando: false }
}
