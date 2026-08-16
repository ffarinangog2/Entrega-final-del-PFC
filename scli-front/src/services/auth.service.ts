import { api } from './api'
import type {
  AuthUser,
  LoginRequest,
  LoginResponse,
} from '@/types/auth'

export const authService = {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>(
      '/api/v1/auth/login',
      credentials,
    )

    return response.data
  },

  async me(): Promise<AuthUser> {
    const response = await api.get<AuthUser>(
      '/api/v1/auth/me',
    )

    return response.data
  },
}