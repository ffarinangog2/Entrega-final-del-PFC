import { useContext, useEffect, useState, type ReactNode } from 'react'
import {
  obtenerPeriodoActual,
  obtenerPeriodos,
  type PeriodoLectivo,
} from './services/academicoApi'
import { AcademicPeriodContext } from './academicPeriodContext'
import { AuthContext } from './auth'
import { ApiError } from './services/apiClient'

export function AcademicPeriodProvider({ children }: { children: ReactNode }) {
  const auth = useContext(AuthContext)
  const isAuthenticated = auth
    ? auth.isAuthenticated
    : Boolean(sessionStorage.getItem('accessToken'))
  const isAuthLoading = auth?.isLoading ?? false
  const [periodos, setPeriodos] = useState<PeriodoLectivo[]>([])
  const [periodoVigente, setPeriodoVigente] = useState<PeriodoLectivo | null>(null)
  const [periodoSeleccionado, setPeriodoSeleccionado] = useState<PeriodoLectivo | null>(null)
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
      setPeriodoSeleccionado(null)
      setCargando(false)
      setError(undefined)
      return () => {
        activo = false
      }
    }
    setCargando(true)
    setError(undefined)

    Promise.allSettled([obtenerPeriodoActual(), obtenerPeriodos()])
      .then(([vigenteResult, periodosResult]) => {
        if (!activo) return

        const vigente =
          vigenteResult.status === 'fulfilled' ? vigenteResult.value : null
        const listaPeriodos =
          periodosResult.status === 'fulfilled' && Array.isArray(periodosResult.value)
            ? periodosResult.value
            : []

        // Unir asegurando que el período vigente esté en la lista sin duplicarlo
        const combinados = [...listaPeriodos]
        if (vigente && !combinados.some((p) => p.id === vigente.id)) {
          combinados.unshift(vigente)
        }

        setPeriodos(combinados)
        setPeriodoVigente(vigente)
        setPeriodoSeleccionado((prev) => {
          if (prev && combinados.some((p) => p.id === prev.id)) {
            return prev
          }
          return vigente ?? (combinados.length > 0 ? combinados[0] : null)
        })

        if (vigenteResult.status === 'rejected') {
          const cause = vigenteResult.reason
          if (!(cause instanceof ApiError && cause.status === 404)) {
            setError(
              cause instanceof Error
                ? cause.message
                : 'No se pudo consultar el período académico actual.',
            )
          }
        }
      })
      .catch((cause) => {
        if (activo) {
          setPeriodos([])
          setPeriodoVigente(null)
          setPeriodoSeleccionado(null)
          if (!(cause instanceof ApiError && cause.status === 404)) {
            setError(
              cause instanceof Error
                ? cause.message
                : 'No se pudo consultar los períodos académicos.',
            )
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

  const seleccionarPeriodo = (id: string) => {
    const encontrado = periodos.find((p) => p.id === id)
    if (encontrado) {
      setPeriodoSeleccionado(encontrado)
    }
  }

  return (
    <AcademicPeriodContext.Provider
      value={{
        periodos,
        periodoVigente,
        periodoSeleccionado: periodoSeleccionado ?? periodoVigente,
        seleccionarPeriodo,
        cargando,
        error,
      }}
    >
      {children}
    </AcademicPeriodContext.Provider>
  )
}
