import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth'

export function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) return <p>Cargando sesión...</p>
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <Outlet />
}