import { FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AuthApiError } from '../services/authApi'
import { useAuth } from '../auth'

function getErrorMessage(error: unknown) {
  if (!(error instanceof AuthApiError)) return 'No se pudo iniciar sesión.'
  if (error.status === 401) return 'Credenciales inválidas.'
  if (error.status === 403) return 'No tienes permisos para acceder.'
  if (error.status === 503) return 'El servicio de autenticación no está disponible.'
  return 'No se pudo iniciar sesión. Inténtalo de nuevo.'
}

export function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!username.trim() || !password) {
      setError('Introduce tu usuario y contraseña.')
      return
    }

    setError('')
    setIsLoading(true)
    try {
      await login(username.trim(), password)
      navigate('/main', { replace: true })
    } catch (loginError) {
      setError(getErrorMessage(loginError))
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <main>
      <h1>Iniciar sesión</h1>
      <p>Acceso al sistema SCLI.</p>
      <form onSubmit={handleSubmit}>
        <label>
          Usuario o correo
          <input
            type="text"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            autoComplete="username"
            required
          />
        </label>
        <label>
          Contraseña
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
        {error && <p role="alert">{error}</p>}
        <button type="submit" disabled={isLoading}>
          {isLoading ? 'Iniciando sesión...' : 'Iniciar sesión'}
        </button>
      </form>
    </main>
  )
}
