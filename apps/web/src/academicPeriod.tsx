import { useContext, useEffect, useState, type ReactNode } from 'react'
import { obtenerPeriodoActual, type PeriodoLectivo } from './services/academicoApi'
import { AcademicPeriodContext } from './academicPeriodContext'
import { AuthContext } from './auth'
import { ApiError } from './services/apiClient'

export function AcademicPeriodProvider({ children }: { children: ReactNode }) {
  const auth = useContext(AuthContext)
  const isAuthenticated = auth ? auth.isAuthenticated : Boolean(sessionStorage.getItem('accessToken'))
  const isAuthLoading = auth?.isLoading ?? false
  const [periodos, setPeriodos] = useState<PeriodoLectivo[]>([])
  const [periodoVigente, setPeriodoVigente] = useState<PeriodoLectivo | null>(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string>()

  useEffect(() => {
    let activo = true
    if (isAuthLoading) {
      return () => {
        activo = false
      }
    }
    if (!isAuthenticated) {
      setPeriodos([])
      setPeriodoVigente(null)
      setCargando(false)
      setError(undefined)
      return () => {
        activo = false
      }
    }
    setCargando(true)
    setError(undefined)
    obtenerPeriodoActual()
      .then((vigente) => {
        if (!activo) return
        setPeriodos([vigente])
        setPeriodoVigente(vigente)
      })
      .catch((cause) => {
        if (activo) {
          setPeriodos([])
          setPeriodoVigente(null)
          if (!(cause instanceof ApiError && cause.status === 404)) {
            setError(cause instanceof Error ? cause.message : 'No se pudo consultar el período académico actual.')
          }
        }
      })
      .finally(() => {
        if (activo) setCargando(false)
      })
    return () => {
      activo = false
    }
  }, [isAuthenticated, isAuthLoading])

  return (
    <AcademicPeriodContext.Provider
      value={{ periodos, periodoVigente, periodoSeleccionado: periodoVigente, seleccionarPeriodo: () => undefined, cargando, error }}
    >
      {children}
    </AcademicPeriodContext.Provider>
  )
}
