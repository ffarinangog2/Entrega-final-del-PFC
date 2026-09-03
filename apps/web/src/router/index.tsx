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
import { PlanificacionPage } from '../features/planificacion/PlanificacionPage'
import { AsistenciaPage } from '../features/asistencia/AsistenciaPage'
import { IncidentesPage } from '../features/incidentes/IncidentesPage'
import { ProfilePage } from '../pages/ProfilePage'
import { NotFoundPage, RouteErrorBoundary } from './RouteFeedback'
import { AdminInstitutionPage } from '../features/admin/AdminInstitutionPage'

export function AppRoutes() {
  return (
    <RouteErrorBoundary><Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/recuperar-contrasena" element={<ForgotPasswordPage />} />
      <Route path="/restablecer-contrasena" element={<ResetPasswordPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/main" element={<MainPage />} />
        <Route element={<ProtectedRoute roles={['ADMINISTRADOR']} />}>
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/about" element={<AboutPage />} />
        </Route>
        <Route element={<ProtectedRoute roles={['ADMINISTRADOR']} permissions={['USUARIO_LEER']} />}>
          <Route path="/usuarios" element={<UsuariosPage />} />
          <Route path="/administracion" element={<AdminInstitutionPage />} />
          <Route path="/laboratorios" element={<AdminInstitutionPage />} />
          <Route path="/pisos" element={<AdminInstitutionPage />} />
          <Route path="/equipos" element={<AdminInstitutionPage />} />
        </Route>
        <Route path="/perfil" element={<ProfilePage />} />
        <Route element={<ProtectedRoute roles={['ADMINISTRADOR', 'ADMINISTRADOR_PISO', 'COORDINADOR']} permissions={['PLANIFICACION_GESTIONAR', 'SOLICITUD_APROBAR']} />}><Route path="/planificacion" element={<PlanificacionPage />} /></Route>
        <Route element={<ProtectedRoute roles={['DOCENTE', 'ESTUDIANTE']} permissions={['RESERVA_LEER', 'ACADEMICO_LEER']} />}><Route path="/asistencia" element={<AsistenciaPage />} /></Route>
        <Route element={<ProtectedRoute roles={['ADMINISTRADOR', 'ADMINISTRADOR_PISO', 'DOCENTE']} permissions={['INCIDENTE_LEER', 'INCIDENTE_CREAR', 'INCIDENTE_GESTIONAR']} />}><Route path="/incidentes" element={<IncidentesPage />} /></Route>
        <Route element={<ProtectedRoute roles={['ADMINISTRADOR', 'ADMINISTRADOR_PISO', 'DOCENTE']} permissions={['RESERVA_LEER', 'SOLICITUD_LEER']} />}>
          <Route path="/reservas" element={<ReservasListPage />} />
          <Route path="/reservas/:id" element={<ReservaDetailPage />} />
          <Route path="/solicitudes/:id" element={<SolicitudDetailPage />} />
        </Route>
        <Route element={<ProtectedRoute roles={['DOCENTE']} permissions={['SOLICITUD_CREAR']} />}>
          <Route path="/reservas/nueva" element={<NuevaSolicitudPage />} />
        </Route>
        <Route element={<ProtectedRoute roles={['ADMINISTRADOR', 'ADMINISTRADOR_PISO', 'COORDINADOR', 'DOCENTE', 'ESTUDIANTE']} permissions={['RESERVA_LEER', 'AGENDA_GESTIONAR', 'ACADEMICO_LEER']} />}>
          <Route path="/reservas/calendario" element={<CalendarioReservasPage />} />
        </Route>
      </Route>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes></RouteErrorBoundary>
  )
}
