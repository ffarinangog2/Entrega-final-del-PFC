import { useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { obtenerPeriodoActual, obtenerPeriodos, type PeriodoLectivo } from './services/academicoApi'
import { AcademicPeriodContext, STORAGE_KEY } from './academicPeriodContext'
import { AuthContext } from './auth'

export function AcademicPeriodProvider({ children }: { children: ReactNode }) {
  const auth = useContext(AuthContext)
  const isAuthenticated = auth ? auth.isAuthenticated : Boolean(sessionStorage.getItem('accessToken'))
  const [periodos, setPeriodos] = useState<PeriodoLectivo[]>([])
  const [periodoVigente, setPeriodoVigente] = useState<PeriodoLectivo | null>(null)
  const [seleccionadoId, setSeleccionadoId] = useState(() => sessionStorage.getItem(STORAGE_KEY) ?? '')
  const [cargando, setCargando] = useState(false)

  useEffect(() => {
    let activo = true
    if (!isAuthenticated) {
      setPeriodos([])
      setPeriodoVigente(null)
      setCargando(false)
      return () => {
        activo = false
      }
    }
    setCargando(true)
    Promise.all([obtenerPeriodos(), obtenerPeriodoActual()])
      .then(([lista, vigente]) => {
        if (!activo) return
        setPeriodos(lista.sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio)))
        setPeriodoVigente(vigente)
        setSeleccionadoId((actual) => (actual && lista.some((p) => p.id === actual) ? actual : vigente.id))
      })
      .catch(() => {
        if (activo) {
          setPeriodos([])
          setPeriodoVigente(null)
        }
      })
      .finally(() => {
        if (activo) setCargando(false)
      })
    return () => {
      activo = false
    }
  }, [isAuthenticated])

  const periodoSeleccionado = useMemo(
    () => periodos.find((p) => p.id === seleccionadoId) ?? periodoVigente,
    [periodos, periodoVigente, seleccionadoId],
  )

  const seleccionarPeriodo = (id: string) => {
    sessionStorage.setItem(STORAGE_KEY, id)
    setSeleccionadoId(id)
  }

  return (
    <AcademicPeriodContext.Provider
      value={{ periodos, periodoVigente, periodoSeleccionado, seleccionarPeriodo, cargando }}
    >
      {children}
    </AcademicPeriodContext.Provider>
  )
}
