import { useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { NavLink } from 'react-router-dom'
import { hasAnyPermission, hasPermission, hasRole, useAuth } from '../auth'
import '../i18n'
import { LogoutButton } from './LogoutButton'
import '../pages/MainPage.css'

export function DashboardLayout({
  breadcrumb,
  children,
}: {
  breadcrumb: string
  children: ReactNode
}) {
  const { usuario } = useAuth()
  const { t } = useTranslation()
  const [menuAbierto, setMenuAbierto] = useState(false)
  const nombre =
    usuario?.nombres || usuario?.username || t('dashboard.userNameFallback')
  const rol = usuario?.roles?.[0] || t('dashboard.userRoleFallback')
  const administrador = hasRole(usuario, 'ADMINISTRADOR')
  const administradorPiso = hasRole(usuario, 'ADMINISTRADOR_PISO')
  const coordinador = hasRole(usuario, 'COORDINADOR')
  const docente = hasRole(usuario, 'DOCENTE')
  const estudiante = hasRole(usuario, 'ESTUDIANTE')
  const verLaboratorios =
    (administrador || administradorPiso || coordinador) &&
    hasAnyPermission(usuario, ['ACADEMICO_LEER', 'LABORATORIO_LEER'])
  const verReservas =
    (administrador || administradorPiso || docente) &&
    hasAnyPermission(usuario, ['RESERVA_LEER', 'SOLICITUD_LEER'])
  const crearSolicitud = docente && hasPermission(usuario, 'SOLICITUD_CREAR')
  const verCalendario =
    (administrador || administradorPiso || coordinador || docente) &&
    hasAnyPermission(usuario, [
      'RESERVA_LEER',
      'AGENDA_GESTIONAR',
      'ACADEMICO_LEER',
    ])
  const verUsuarios = administrador && hasPermission(usuario, 'USUARIO_LEER')
  const verPlanificacion =
    (administrador || administradorPiso || coordinador) &&
    hasAnyPermission(usuario, ['PLANIFICACION_GESTIONAR', 'SOLICITUD_APROBAR'])
  const verAsistencia =
    (docente || estudiante) &&
    hasAnyPermission(usuario, ['RESERVA_LEER', 'ACADEMICO_LEER'])
  const verIncidentes =
    (administrador || administradorPiso || docente) &&
    hasAnyPermission(usuario, [
      'INCIDENTE_LEER',
      'INCIDENTE_CREAR',
      'INCIDENTE_GESTIONAR',
    ])
  return (
    <div className="dashboard">
      <header className="dashboard__topbar">
        <div className="dashboard__identity">
          <button
            className="dashboard__menu"
            aria-label="Abrir navegación"
            aria-expanded={menuAbierto}
            onClick={() => setMenuAbierto((x) => !x)}
          >
            ☰
          </button>
          <span className="dashboard__logo">S</span>
          <div>
            <strong>SCLI</strong>
            <span>{t('dashboard.topbar.tagline')}</span>
          </div>
        </div>
        <div className="dashboard__account">
          <div className="dashboard__avatar" aria-hidden="true">
            {nombre.charAt(0).toUpperCase()}
          </div>
          <div className="dashboard__user-copy">
            <strong>{nombre}</strong>
            <span>{rol.replace(/_/g, ' ')}</span>
          </div>
          <LogoutButton />
        </div>
      </header>
      <div className="dashboard__body">
        <aside
          className={`dashboard__sidebar ${menuAbierto ? 'dashboard__sidebar--open' : ''}`}
        >
          <p className="dashboard__nav-label">
            {t('dashboard.sidebar.workspaceLabel')}
          </p>
          <nav aria-label="Navegación principal">
            <NavLink className="dashboard__nav-item" to="/main" end>
              <span aria-hidden="true">⌂</span>
              {t('dashboard.nav.home')}
            </NavLink>
            {verLaboratorios && (
              <NavLink className="dashboard__nav-item" to={administrador ? '/laboratorios' : '/main'}>
                <span aria-hidden="true">L</span>
                {t('dashboard.nav.labs')}
              </NavLink>
            )}
            {verReservas && (
              <NavLink className="dashboard__nav-item" to="/reservas">
                <span aria-hidden="true">R</span>
                {t('dashboard.nav.reservations')}
              </NavLink>
            )}
            {crearSolicitud && (
              <NavLink className="dashboard__nav-item" to="/reservas/nueva">
                <span aria-hidden="true">+</span>
                {t('dashboard.nav.newRequest')}
              </NavLink>
            )}
            {verCalendario && (
              <NavLink
                className="dashboard__nav-item"
                to="/reservas/calendario"
              >
                <span aria-hidden="true">C</span>
                {coordinador
                  ? 'Disponibilidad de laboratorios'
                  : t('dashboard.nav.calendar')}
              </NavLink>
            )}
            {verPlanificacion && (
              <NavLink className="dashboard__nav-item" to="/planificacion">
                <span aria-hidden="true">P</span>Planificación
              </NavLink>
            )}
            {verAsistencia && (
              <NavLink className="dashboard__nav-item" to="/asistencia">
                <span aria-hidden="true">A</span>
                {estudiante ? 'Registro e historial' : 'Asistencia'}
              </NavLink>
            )}
            {verIncidentes && (
              <NavLink className="dashboard__nav-item" to="/incidentes">
                <span aria-hidden="true">!</span>Incidentes
              </NavLink>
            )}
          </nav>
          <p className="dashboard__nav-label dashboard__nav-label--secondary">
            {t('dashboard.sidebar.systemLabel')}
          </p>
          <nav aria-label="Navegación del sistema">
            {verUsuarios && (
              <NavLink className="dashboard__nav-item" to="/usuarios">
                <span aria-hidden="true">U</span>
                {t('dashboard.nav.usuarios')}
              </NavLink>
            )}
            {administrador && (
              <NavLink className="dashboard__nav-item" to="/administracion">
                <span aria-hidden="true">G</span>
                Administración
              </NavLink>
            )}
            {administrador && (
              <NavLink className="dashboard__nav-item" to="/pisos">
                <span aria-hidden="true">P</span>Pisos
              </NavLink>
            )}
            {administrador && (
              <NavLink className="dashboard__nav-item" to="/equipos">
                <span aria-hidden="true">E</span>Equipos
              </NavLink>
            )}
            {administrador && (
              <NavLink className="dashboard__nav-item" to="/catalogos">
                <span aria-hidden="true">C</span>Catálogos
              </NavLink>
            )}
            {administrador && (
              <NavLink className="dashboard__nav-item" to="/asignaciones">
                <span aria-hidden="true">A</span>Asignaciones
              </NavLink>
            )}
            <NavLink className="dashboard__nav-item" to="/perfil">
              <span aria-hidden="true">●</span>Mi perfil
            </NavLink>
            {administrador && (
              <NavLink className="dashboard__nav-item" to="/settings">
                <span aria-hidden="true">⚙</span>
                {t('dashboard.nav.settings')}
              </NavLink>
            )}
            {administrador && (
              <NavLink className="dashboard__nav-item" to="/about">
                <span aria-hidden="true">i</span>
                {t('dashboard.nav.about')}
              </NavLink>
            )}
          </nav>
          <div className="dashboard__sidebar-note">
            <span className="dashboard__status-dot" />
            Servicios conectados
          </div>
        </aside>
        <main className="dashboard__content">
          <div className="dashboard__breadcrumb">
            {t('dashboard.breadcrumbHome')} <span>/</span> {breadcrumb}
          </div>
          {children}
        </main>
      </div>
    </div>
  )
}
