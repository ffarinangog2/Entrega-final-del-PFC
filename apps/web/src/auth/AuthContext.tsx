import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { login as loginRequest, refresh } from '../services/authApi'
import type { AuthUser, LoginResponse } from '../types/auth'
import { AuthContext } from './context'

const ACCESS_TOKEN_KEY = 'accessToken'
const REFRESH_TOKEN_KEY = 'refreshToken'
const EXPIRES_AT_KEY = 'expiresAt'
const USER_KEY = 'usuario'

function clearStoredSession() {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  sessionStorage.removeItem(EXPIRES_AT_KEY)
  sessionStorage.removeItem(USER_KEY)
}

function storeSession(response: LoginResponse) {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken)
  sessionStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken)
  sessionStorage.setItem(
    EXPIRES_AT_KEY,
    String(Date.now() + response.expiresIn * 1000),
  )
  sessionStorage.setItem(USER_KEY, JSON.stringify(response.usuario))
}

function restoreUser() {
  const expiresAt = Number(sessionStorage.getItem(EXPIRES_AT_KEY))
  const storedUser = sessionStorage.getItem(USER_KEY)
  if (!expiresAt || expiresAt <= Date.now() || !storedUser) {
    clearStoredSession()
    return null
  }

  try {
    return JSON.parse(storedUser) as AuthUser
  } catch {
    clearStoredSession()
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<AuthUser | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    setUsuario(restoreUser())
    setIsLoading(false)
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    const response = await loginRequest(username, password)
    storeSession(response)
    setUsuario(response.usuario)
  }, [])

  const logout = useCallback(() => {
    clearStoredSession()
    setUsuario(null)
  }, [])

  const refreshSession = useCallback(async () => {
    const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY)
    if (!refreshToken) {
      logout()
      return false
    }

    try {
      const response = await refresh(refreshToken)
      storeSession(response)
      setUsuario(response.usuario)
      return true
    } catch {
      logout()
      return false
    }
  }, [logout])

  const value = useMemo(
    () => ({
      usuario,
      isAuthenticated: usuario !== null,
      isLoading,
      login,
      logout,
      refreshSession,
    }),
    [isLoading, login, logout, refreshSession, usuario],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

