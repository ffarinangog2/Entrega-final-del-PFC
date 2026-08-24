import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { NavLink } from 'react-router-dom'
import { useAuth } from '../auth'
import '../i18n'
import { LogoutButton } from './LogoutButton'
import '../pages/MainPage.css'

export function DashboardLayout({ breadcrumb, children }: { breadcrumb: string; children: ReactNode }) {
  const { usuario } = useAuth()
  const { t } = useTranslation()
  const nombre = usuario?.nombres || usuario?.username || t('dashboard.userNameFallback')
  const rol = usuario?.roles?.[0] || t('dashboard.userRoleFallback')
  return <div className="dashboard">
    <header className="dashboard__topbar">
      <div className="dashboard__identity"><span className="dashboard__logo">S</span><div><strong>SCLI</strong><span>{t('dashboard.topbar.tagline')}</span></div></div>
      <div className="dashboard__account"><div className="dashboard__avatar" aria-hidden="true">{nombre.charAt(0).toUpperCase()}</div><div className="dashboard__user-copy"><strong>{nombre}</strong><span>{rol}</span></div><LogoutButton /></div>
    </header>
    <div className="dashboard__body">
      <aside className="dashboard__sidebar">
        <p className="dashboard__nav-label">{t('dashboard.sidebar.workspaceLabel')}</p>
        <nav aria-label="Navegación principal">
          <NavLink className="dashboard__nav-item" to="/main" end><span aria-hidden="true">⌂</span>{t('dashboard.nav.home')}</NavLink>
          <NavLink className="dashboard__nav-item" to="/main"><span aria-hidden="true">◦</span>{t('dashboard.nav.labs')}</NavLink>
          <NavLink className="dashboard__nav-item" to="/reservas"><span aria-hidden="true">◫</span>{t('dashboard.nav.reservations')}</NavLink>
          <NavLink className="dashboard__nav-item" to="/reservas/nueva"><span aria-hidden="true">+</span>{t('dashboard.nav.newRequest')}</NavLink>
          <NavLink className="dashboard__nav-item" to="/reservas/calendario"><span aria-hidden="true">▦</span>{t('dashboard.nav.calendar')}</NavLink>
        </nav>
        <p className="dashboard__nav-label dashboard__nav-label--secondary">{t('dashboard.sidebar.systemLabel')}</p>
        <nav aria-label="Navegación del sistema">
          <NavLink className="dashboard__nav-item" to="/settings"><span aria-hidden="true">⚙</span>{t('dashboard.nav.settings')}</NavLink>
          <NavLink className="dashboard__nav-item" to="/about"><span aria-hidden="true">ⓘ</span>{t('dashboard.nav.about')}</NavLink>
        </nav>
        <div className="dashboard__sidebar-note"><span className="dashboard__status-dot" />{t('dashboard.sidebar.note')}</div>
      </aside>
      <main className="dashboard__content"><div className="dashboard__breadcrumb">{t('dashboard.breadcrumbHome')} <span>/</span> {breadcrumb}</div>{children}</main>
    </div>
  </div>
}
