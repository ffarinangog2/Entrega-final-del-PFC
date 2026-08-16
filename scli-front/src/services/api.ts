import axios from 'axios'

const baseURL = import.meta.env.VITE_AUTH_API_URL

if (!baseURL) {
  throw new Error('La variable VITE_AUTH_API_URL no está configurada')
}

export const api = axios.create({
  baseURL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('scli_access_token')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (
      error.response?.status === 401 &&
      error.config?.url !== '/api/v1/auth/login'
    ) {
      sessionStorage.removeItem('scli_access_token')
      sessionStorage.removeItem('scli_auth_user')
    }

    return Promise.reject(error)
  },
)