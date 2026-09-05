import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth'
import { hasAnyPermission, hasRole } from '../auth'
import { AccessDeniedPage } from './RouteFeedback'

export function ProtectedRoute({
  permissions = [],
  roles = [],
}: {
  permissions?: readonly string[]
  roles?: readonly string[]
}) {
  const { isAuthenticated, isLoading, usuario } = useAuth()

  if (isLoading) return <p>Cargando sesión...</p>
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (permissions.length > 0 && !hasAnyPermission(usuario, permissions)) {
    return <AccessDeniedPage />
  }
  if (roles.length > 0 && !roles.some((role) => hasRole(usuario, role))) {
    return <AccessDeniedPage />
  }
  return <Outlet />
}
