import type { ReactNode } from 'react'
import { NavLink } from 'react-router-dom'
import { useAuth } from '../auth'
import { LogoutButton } from './LogoutButton'
import '../pages/MainPage.css'

export function DashboardLayout({ breadcrumb, children }: { breadcrumb: string; children: ReactNode }) {
  const { usuario } = useAuth()
  const nombre = usuario?.nombres || usuario?.username || 'Usuario'
  const rol = usuario?.roles?.[0] || 'Usuario del sistema'
  return <div className="dashboard">
    <header className="dashboard__topbar">
      <div className="dashboard__identity"><span className="dashboard__logo">S</span><div><strong>SCLI</strong><span>Laboratorios Informáticos</span></div></div>
      <div className="dashboard__account"><div className="dashboard__avatar" aria-hidden="true">{nombre.charAt(0).toUpperCase()}</div><div className="dashboard__user-copy"><strong>{nombre}</strong><span>{rol}</span></div><LogoutButton /></div>
    </header>
    <div className="dashboard__body">
      <aside className="dashboard__sidebar">
        <p className="dashboard__nav-label">Espacios de trabajo</p>
        <nav aria-label="Navegación principal">
          <NavLink className="dashboard__nav-item" to="/main" end><span aria-hidden="true">⌂</span>Inicio</NavLink>
          <NavLink className="dashboard__nav-item" to="/main"><span aria-hidden="true">◦</span>Laboratorios</NavLink>
          <NavLink className="dashboard__nav-item" to="/reservas"><span aria-hidden="true">◫</span>Reservas</NavLink>
          <NavLink className="dashboard__nav-item" to="/reservas/nueva"><span aria-hidden="true">+</span>Nueva solicitud</NavLink>
          <NavLink className="dashboard__nav-item" to="/reservas/calendario"><span aria-hidden="true">▦</span>Calendario</NavLink>
        </nav>
        <p className="dashboard__nav-label dashboard__nav-label--secondary">Sistema</p>
        <nav aria-label="Navegación del sistema">
          <NavLink className="dashboard__nav-item" to="/settings"><span aria-hidden="true">⚙</span>Configuración</NavLink>
          <NavLink className="dashboard__nav-item" to="/about"><span aria-hidden="true">ⓘ</span>Acerca de</NavLink>
        </nav>
        <div className="dashboard__sidebar-note"><span className="dashboard__status-dot" />Servicios operativos</div>
      </aside>
      <main className="dashboard__content"><div className="dashboard__breadcrumb">Inicio <span>/</span> {breadcrumb}</div>{children}</main>
    </div>
  </div>
}
