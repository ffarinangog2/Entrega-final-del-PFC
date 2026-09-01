import { useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { NavLink } from 'react-router-dom'
import { hasAnyPermission, hasPermission, useAuth } from '../auth'
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
  const verLaboratorios = hasAnyPermission(usuario, [
    'ACADEMICO_LEER',
    'LABORATORIO_LEER',
  ])
  const verReservas = hasAnyPermission(usuario, [
    'RESERVA_LEER',
    'SOLICITUD_LEER',
  ])
  const crearSolicitud = hasPermission(usuario, 'SOLICITUD_CREAR')
  const verCalendario = hasAnyPermission(usuario, [
    'RESERVA_LEER',
    'AGENDA_GESTIONAR',
  ])
  const verUsuarios = hasPermission(usuario, 'USUARIO_LEER')
  const verPlanificacion = hasAnyPermission(usuario, [
    'PLANIFICACION_GESTIONAR',
    'SOLICITUD_APROBAR',
  ])
  const verAsistencia = hasAnyPermission(usuario, [
    'RESERVA_LEER',
    'ACADEMICO_LEER',
  ])
  const verIncidentes = hasAnyPermission(usuario, [
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
            aria-label="Abrir navegaciÃ³n"
            aria-expanded={menuAbierto}
            onClick={() => setMenuAbierto((x) => !x)}
          >
            â˜°
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
              <span>âŒ‚</span>
              {t('dashboard.nav.home')}
            </NavLink>
            {verLaboratorios && (
              <NavLink className="dashboard__nav-item" to="/main">
                <span>â—¦</span>
                {t('dashboard.nav.labs')}
              </NavLink>
            )}
            {verReservas && (
              <NavLink className="dashboard__nav-item" to="/reservas">
                <span>â—«</span>
                {t('dashboard.nav.reservations')}
              </NavLink>
            )}
            {crearSolicitud && (
              <NavLink className="dashboard__nav-item" to="/reservas/nueva">
                <span>+</span>
                {t('dashboard.nav.newRequest')}
              </NavLink>
            )}
            {verCalendario && (
              <NavLink
                className="dashboard__nav-item"
                to="/reservas/calendario"
              >
                <span>â–¦</span>
                {t('dashboard.nav.calendar')}
              </NavLink>
            )}
            {verPlanificacion && (
              <NavLink className="dashboard__nav-item" to="/planificacion">
                <span>P</span>PlanificaciÃ³n
              </NavLink>
            )}
            {verAsistencia && (
              <NavLink className="dashboard__nav-item" to="/asistencia">
                <span>A</span>Asistencia
              </NavLink>
            )}
            {verIncidentes && (
              <NavLink className="dashboard__nav-item" to="/incidentes">
                <span>!</span>Incidentes
              </NavLink>
            )}
          </nav>
          <p className="dashboard__nav-label dashboard__nav-label--secondary">
            {t('dashboard.sidebar.systemLabel')}
          </p>
          <nav aria-label="NavegaciÃ³n del sistema">
            {verUsuarios && (
              <NavLink className="dashboard__nav-item" to="/usuarios">
                <span>U</span>
                {t('dashboard.nav.usuarios')}
              </NavLink>
            )}
            <NavLink className="dashboard__nav-item" to="/perfil">
              <span>â—‹</span>Mi perfil
            </NavLink>
            <NavLink className="dashboard__nav-item" to="/settings">
              <span>âš™</span>
              {t('dashboard.nav.settings')}
            </NavLink>
            <NavLink className="dashboard__nav-item" to="/about">
              <span>i</span>
              {t('dashboard.nav.about')}
            </NavLink>
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
