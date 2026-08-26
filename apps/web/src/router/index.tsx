import { Navigate, Route, Routes } from 'react-router-dom'
import { AboutPage } from '../pages/AboutPage'
import { LoginPage } from '../pages/LoginPage'
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage'
import { ResetPasswordPage } from '../pages/ResetPasswordPage'
import { MainPage } from '../pages/MainPage'
import { SettingsPage } from '../pages/SettingsPage'
import { UsuariosPage } from '../pages/UsuariosPage'
import { ProtectedRoute } from './ProtectedRoute'
import { ReservaDetailPage } from '../features/reservas/ReservaDetailPage'
import { ReservasListPage } from '../features/reservas/ReservasListPage'
import { CalendarioReservasPage } from '../features/reservas/CalendarioReservasPage'
import { NuevaSolicitudPage } from '../features/reservas/NuevaSolicitudPage'
import { SolicitudDetailPage } from '../features/reservas/SolicitudDetailPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/recuperar-contrasena" element={<ForgotPasswordPage />} />
      <Route path="/restablecer-contrasena" element={<ResetPasswordPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/main" element={<MainPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="/usuarios" element={<UsuariosPage />} />
        <Route element={<ProtectedRoute permissions={['RESERVA_LEER', 'SOLICITUD_LEER']} />}>
          <Route path="/reservas" element={<ReservasListPage />} />
          <Route path="/reservas/:id" element={<ReservaDetailPage />} />
          <Route path="/solicitudes/:id" element={<SolicitudDetailPage />} />
        </Route>
        <Route element={<ProtectedRoute permissions={['SOLICITUD_CREAR']} />}>
          <Route path="/reservas/nueva" element={<NuevaSolicitudPage />} />
        </Route>
        <Route element={<ProtectedRoute permissions={['RESERVA_LEER', 'AGENDA_GESTIONAR']} />}>
          <Route path="/reservas/calendario" element={<CalendarioReservasPage />} />
        </Route>
      </Route>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
