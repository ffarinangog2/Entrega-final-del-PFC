import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth'

export function LogoutButton() {
  const navigate = useNavigate()
  const { logout } = useAuth()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return <button onClick={handleLogout}>Cerrar sesión</button>
}