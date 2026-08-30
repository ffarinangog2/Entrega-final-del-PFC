import type {
  LoginRequest,
  LoginResponse,
  RefreshTokenRequest,
} from '../types/auth'

const API_URL = (import.meta.env.VITE_API_URL || '').replace(
  /\/$/,
  '',
)

export class AuthApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message)
    this.name = 'AuthApiError'
  }
}

async function request<T>(path: string, init: RequestInit): Promise<T> {
  let response: Response

  try {
    response = await fetch(`${API_URL}${path}`, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...init.headers,
      },
    })
  } catch {
    throw new AuthApiError(503, 'El servicio de autenticación no está disponible.')
  }

  if (!response.ok) {
    let message = 'No se pudo completar la solicitud.'
    try {
      const errorBody = (await response.json()) as { message?: string }
      if (errorBody.message) message = errorBody.message
    } catch {
      // Some gateway errors do not include a JSON body.
    }
    throw new AuthApiError(response.status, message)
  }

  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export function login(username: string, password: string) {
  const body: LoginRequest = { username, password }
  return request<LoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function refresh(refreshToken: string) {
  const body: RefreshTokenRequest = { refreshToken }
  return request<LoginResponse>('/api/v1/auth/refresh', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function logout(refreshToken: string) {
  const body: RefreshTokenRequest = { refreshToken }
  return request<void>('/api/v1/auth/logout', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function forgotPassword(identifier: string) {
  return request<{ message: string }>('/api/v1/auth/forgot-password', {
    method: 'POST', body: JSON.stringify({ identifier }),
  })
}

export function resetPassword(token: string, newPassword: string, confirmPassword: string) {
  return request<{ message: string }>('/api/v1/auth/reset-password', {
    method: 'POST', body: JSON.stringify({ token, newPassword, confirmPassword }),
  })
}
