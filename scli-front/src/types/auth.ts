export interface LoginRequest {
  username: string
  password: string
}

export interface AuthUser {
  id: string
  perfilId: string
  username: string
  roles: string[]
  permisos: string[]
}

export interface LoginResponse {
  tokenType: string
  accessToken: string
  expiresIn: number
  usuario: AuthUser
}

export interface ApiErrorResponse {
  timestamp?: string
  status?: number
  error?: string
  message?: string
  path?: string
}