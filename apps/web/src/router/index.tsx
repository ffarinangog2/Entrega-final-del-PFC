import { Navigate, Route, Routes } from 'react-router-dom'
import { AboutPage } from '../pages/AboutPage'
import { LoginPage } from '../pages/LoginPage'
import { MainPage } from '../pages/MainPage'
import { SettingsPage } from '../pages/SettingsPage'
import { UsuariosPage } from '../pages/UsuariosPage'
import { ProtectedRoute } from './ProtectedRoute'
import { ReservaDetailPage } from '../features/reservas/ReservaDetailPage'
import { ReservasListPage } from '../features/reservas/ReservasListPage'
import { CalendarioReservasPage } from '../features/reservas/CalendarioReservasPage'
import { NuevaSolicitudPage } from '../features/reservas/NuevaSolicitudPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/main" element={<MainPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="/usuarios" element={<UsuariosPage />} />
        <Route path="/reservas" element={<ReservasListPage />} />
        <Route path="/reservas/nueva" element={<NuevaSolicitudPage />} />
        <Route path="/reservas/calendario" element={<CalendarioReservasPage />} />
        <Route path="/reservas/:id" element={<ReservaDetailPage />} />
      </Route>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
