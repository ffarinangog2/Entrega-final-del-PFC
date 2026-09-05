import { useContext, useEffect, useState, type ReactNode } from 'react'
import { obtenerPeriodoActual, type PeriodoLectivo } from './services/academicoApi'
import { AcademicPeriodContext } from './academicPeriodContext'
import { AuthContext } from './auth'

export function AcademicPeriodProvider({ children }: { children: ReactNode }) {
  const auth = useContext(AuthContext)
  const isAuthenticated = auth ? auth.isAuthenticated : Boolean(sessionStorage.getItem('accessToken'))
  const [periodos, setPeriodos] = useState<PeriodoLectivo[]>([])
  const [periodoVigente, setPeriodoVigente] = useState<PeriodoLectivo | null>(null)
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
    obtenerPeriodoActual()
      .then((vigente) => {
        if (!activo) return
        setPeriodos([vigente])
        setPeriodoVigente(vigente)
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

  return (
    <AcademicPeriodContext.Provider
      value={{ periodos, periodoVigente, periodoSeleccionado: periodoVigente, seleccionarPeriodo: () => undefined, cargando }}
    >
      {children}
    </AcademicPeriodContext.Provider>
  )
}
