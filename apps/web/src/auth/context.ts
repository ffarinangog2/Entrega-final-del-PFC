import { createContext } from 'react'
import type { AuthUser } from '../types/auth'

export interface AuthContextValue {
  usuario: AuthUser | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  refreshSession: () => Promise<boolean>
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)