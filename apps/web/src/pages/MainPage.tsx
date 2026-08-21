import { LaboratoriosPanel } from '../features/laboratorios/LaboratoriosPanel'
import { LogoutButton } from '../components/LogoutButton'
import { useAuth } from '../auth'
import { NavLink } from 'react-router-dom'
import './MainPage.css'

export function MainPage() {
  const { usuario } = useAuth()
  const nombre = usuario?.nombres || usuario?.username || 'Usuario'
  const rol = usuario?.roles?.[0] || 'Usuario del sistema'

  return (
    <div className="dashboard">
      <header className="dashboard__topbar">
        <div className="dashboard__identity">
          <span className="dashboard__logo">S</span>
          <div>
            <strong>SCLI</strong>
            <span>Laboratorios Informáticos</span>
          </div>
        </div>
        <div className="dashboard__account">
          <div className="dashboard__avatar" aria-hidden="true">
            {nombre.charAt(0).toUpperCase()}
          </div>
          <div className="dashboard__user-copy">
            <strong>{nombre}</strong>
            <span>{rol}</span>
          </div>
          <LogoutButton />
        </div>
      </header>
      <div className="dashboard__body">
        <aside className="dashboard__sidebar">
          <p className="dashboard__nav-label">Espacios de trabajo</p>
          <nav aria-label="Navegación principal">
            <NavLink className="dashboard__nav-item" to="/main" end>
              <span aria-hidden="true">⌂</span>
              Inicio
            </NavLink>
            <NavLink className="dashboard__nav-item" to="/main">
              <span aria-hidden="true">▦</span>
              Laboratorios
            </NavLink>
            <span className="dashboard__nav-item dashboard__nav-item--disabled" aria-disabled="true">
              <span aria-hidden="true">◫</span>
              Reservas
              <small>Próximamente</small>
            </span>
          </nav>
          <p className="dashboard__nav-label dashboard__nav-label--secondary">Sistema</p>
          <nav aria-label="Navegación del sistema">
            <NavLink className="dashboard__nav-item" to="/settings">
              <span aria-hidden="true">⚙</span>
              Configuración
            </NavLink>
            <NavLink className="dashboard__nav-item" to="/about">
              <span aria-hidden="true">ⓘ</span>
              Acerca de
            </NavLink>
          </nav>
          <div className="dashboard__sidebar-note">
            <span className="dashboard__status-dot" />
            Servicios operativos
          </div>
        </aside>
        <main className="dashboard__content">
          <div className="dashboard__breadcrumb">Inicio <span>/</span> Laboratorios</div>
          <LaboratoriosPanel />
        </main>
      </div>
    </div>
  )
}