import { FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AuthApiError } from '../services/authApi'
import { useAuth } from '../auth'
import './LoginPage.css'

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
    <main className="login-page">
      <div className="login-page__backdrop" aria-hidden="true" />
      <section className="login-card" aria-labelledby="login-title">
        <div className="login-card__brand">
          <span className="login-card__mark">S</span>
          <div>
            <p className="login-card__eyebrow">SCLI</p>
            <p className="login-card__brand-name">UTEQ</p>
          </div>
        </div>
        <div className="login-card__heading">
          <p className="login-card__kicker">Acceso institucional</p>
          <h1 id="login-title">SCLI</h1>
          <p>Sistema de Control de Laboratorios Informáticos</p>
        </div>
        <form className="login-form" onSubmit={handleSubmit}>
          <div className="login-form__field">
            <label htmlFor="username">Usuario o correo</label>
          <input
            id="username"
            type="text"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            autoComplete="username"
            required
          />
          </div>
          <div className="login-form__field">
            <label htmlFor="password">Contraseña</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            required
          />
          </div>
          {error && (
            <p className="login-form__error" role="alert">
              {error}
            </p>
          )}
          <button className="login-form__submit" type="submit" disabled={isLoading}>
            {isLoading && <span className="login-form__spinner" aria-hidden="true" />}
            {isLoading ? 'Iniciando sesión...' : 'Iniciar sesión'}
          </button>
        </form>
        <p className="login-card__footer">UTEQ · Aplicaciones Distribuidas</p>
      </section>
    </main>
  )
}
