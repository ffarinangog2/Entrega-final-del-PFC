export interface LoginRequest {
  username: string
  password: string
}

export interface AuthUser {
  id: string
  perfilId: string
  username: string
  nombres: string
  apellidos: string
  emailInstitucional: string
  roles: string[]
  permisos: string[]
  tiposPerfil: string[]
}

export interface LoginResponse {
  tokenType: string
  accessToken: string
  refreshToken: string
  expiresIn: number
  usuario: AuthUser
}

export interface RefreshTokenRequest {
  refreshToken: string
}