import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { obtenerPeriodoActual, obtenerPeriodos, type PeriodoLectivo } from './services/academicoApi'
import { AcademicPeriodContext, STORAGE_KEY } from './academicPeriodContext'

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

  return (
    <AcademicPeriodContext.Provider value={{ periodos, periodoVigente, periodoSeleccionado, seleccionarPeriodo, cargando }}>
      {children}
    </AcademicPeriodContext.Provider>
  )
}
