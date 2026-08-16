import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import axios from 'axios'
import { authService } from '@/services/auth.service'
import type {
  ApiErrorResponse,
  AuthUser,
  LoginRequest,
} from '@/types/auth'

const TOKEN_KEY = 'scli_access_token'
const USER_KEY = 'scli_auth_user'

function loadStoredUser(): AuthUser | null {
  const storedUser = sessionStorage.getItem(USER_KEY)

  if (!storedUser) {
    return null
  }

  try {
    return JSON.parse(storedUser) as AuthUser
  } catch {
    sessionStorage.removeItem(USER_KEY)
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(
    sessionStorage.getItem(TOKEN_KEY),
  )

  const usuario = ref<AuthUser | null>(
    loadStoredUser(),
  )

  const cargando = ref(false)
  const error = ref<string | null>(null)

  const autenticado = computed(
    () => Boolean(accessToken.value && usuario.value),
  )

  async function login(credentials: LoginRequest): Promise<void> {
    cargando.value = true
    error.value = null

    try {
      const response = await authService.login(credentials)

      accessToken.value = response.accessToken
      usuario.value = response.usuario

      sessionStorage.setItem(
        TOKEN_KEY,
        response.accessToken,
      )

      sessionStorage.setItem(
        USER_KEY,
        JSON.stringify(response.usuario),
      )
    } catch (exception: unknown) {
      limpiarSesion()

      if (axios.isAxiosError<ApiErrorResponse>(exception)) {
        error.value =
          exception.response?.data?.message ??
          'No fue posible iniciar sesión'
      } else {
        error.value = 'Ocurrió un error inesperado'
      }

      throw exception
    } finally {
      cargando.value = false
    }
  }

  async function cargarUsuario(): Promise<void> {
    if (!accessToken.value) {
      limpiarSesion()
      return
    }

    try {
      usuario.value = await authService.me()

      sessionStorage.setItem(
        USER_KEY,
        JSON.stringify(usuario.value),
      )
    } catch {
      limpiarSesion()
      throw new Error('La sesión ya no es válida')
    }
  }

  function limpiarSesion(): void {
    accessToken.value = null
    usuario.value = null
    error.value = null

    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(USER_KEY)
  }

  function logout(): void {
    limpiarSesion()
  }

  return {
    accessToken,
    usuario,
    cargando,
    error,
    autenticado,
    login,
    cargarUsuario,
    logout,
  }
})