import { useCallback, useContext, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthContext } from '../../auth'
import {
  listarNotificaciones,
  marcarNotificacionLeida,
  obtenerNotificacionesNoLeidas,
  type NotificacionInterna,
} from '../../services/operationalApi'
import { resolverDestinoNotificacion } from '../notificaciones/notificationNavigation'
import './StudentNotificationBell.css'

export function StudentNotificationBell(props?: { asistencia?: boolean }) {
  void props
  const auth = useContext(AuthContext)
  const navigate = useNavigate()
  const authenticated = auth ? auth.isAuthenticated : true
  const [abierta, setAbierta] = useState(false)
  const [notificaciones, setNotificaciones] = useState<NotificacionInterna[]>([])
  const [conteoNoLeidas, setConteoNoLeidas] = useState<number>(0)
  const bellRef = useRef<HTMLDivElement>(null)

  const refrescarNotificaciones = useCallback(() => {
    if (!authenticated) return
    void Promise.all([
      listarNotificaciones().catch(() => []),
      obtenerNotificacionesNoLeidas().catch(() => null),
    ]).then(([items, noLeidasRes]) => {
      const lista = items ?? []
      setNotificaciones(lista)
      if (noLeidasRes && typeof noLeidasRes.cantidad === 'number') {
        setConteoNoLeidas(noLeidasRes.cantidad)
      } else {
        setConteoNoLeidas(lista.filter((item) => !item.leida).length)
      }
    })
  }, [authenticated])

  useEffect(() => {
    let activo = true
    if (!authenticated) {
      setNotificaciones([])
      setConteoNoLeidas(0)
      return () => {
        activo = false
      }
    }
    Promise.all([
      listarNotificaciones().catch(() => []),
      obtenerNotificacionesNoLeidas().catch(() => null),
    ]).then(([items, noLeidasRes]) => {
      if (!activo) return
      const lista = items ?? []
      setNotificaciones(lista)
      if (noLeidasRes && typeof noLeidasRes.cantidad === 'number') {
        setConteoNoLeidas(noLeidasRes.cantidad)
      } else {
        setConteoNoLeidas(lista.filter((item) => !item.leida).length)
      }
    })
    return () => {
      activo = false
    }
  }, [authenticated])

  useEffect(() => {
    if (!authenticated) return undefined
    const alRecuperarFoco = () => refrescarNotificaciones()
    window.addEventListener('focus', alRecuperarFoco)
    window.addEventListener('notificaciones-actualizadas', alRecuperarFoco)
    const polling = window.setInterval(refrescarNotificaciones, 45_000)
    return () => {
      window.removeEventListener('focus', alRecuperarFoco)
      window.removeEventListener('notificaciones-actualizadas', alRecuperarFoco)
      window.clearInterval(polling)
    }
  }, [authenticated, refrescarNotificaciones])

  useEffect(() => {
    if (!abierta) return undefined
    const alHacerClicFuera = (e: MouseEvent) => {
      if (bellRef.current && !bellRef.current.contains(e.target as Node)) {
        setAbierta(false)
      }
    }
    document.addEventListener('mousedown', alHacerClicFuera)
    return () => document.removeEventListener('mousedown', alHacerClicFuera)
  }, [abierta])

  const noLeidas = notificaciones
    .filter((item) => !item.leida)
    .sort((a, b) => new Date(b.creadaEn).getTime() - new Date(a.creadaEn).getTime())
  const pendientesCount = conteoNoLeidas
  const pendientesVisibles = noLeidas.slice(0, 5)

  const handleClickNotificacion = async (item: NotificacionInterna) => {
    try {
      await marcarNotificacionLeida(item.id)
    } catch {
      // no bloquear el flujo del usuario si la red falla
    }
    setNotificaciones((prev) =>
      prev.map((n) => (n.id === item.id ? { ...n, leida: true } : n)),
    )
    setConteoNoLeidas((prev) => Math.max(0, prev - 1))
    window.dispatchEvent(new Event('notificaciones-actualizadas'))
    setAbierta(false)
    const destino = resolverDestinoNotificacion(item, auth?.usuario)
    navigate(destino)
  }

  return (
    <div className="student-bell" ref={bellRef}>
      <button
        type="button"
        className="student-bell__trigger"
        aria-label={`Notificaciones: ${pendientesCount} pendientes`}
        onClick={() => setAbierta((value) => !value)}
      >
        <span aria-hidden="true">🔔</span>
        {pendientesCount > 0 && (
          <span className="student-bell__badge">{pendientesCount}</span>
        )}
      </button>
      {abierta && (
        <div className="student-bell__popup" role="dialog" aria-label="Notificaciones">
          <header className="student-bell__header">
            <span className="student-bell__header-icon" aria-hidden="true">🔔</span>
            <strong className="student-bell__header-title">Notificaciones</strong>
            {pendientesCount > 0 && (
              <span className="student-bell__header-badge">{pendientesCount}</span>
            )}
          </header>
          <div className="student-bell__body">
            {pendientesVisibles.length === 0 ? (
              <div className="student-bell__empty">
                <span className="student-bell__empty-icon" aria-hidden="true">🔔</span>
                <p>No hay notificaciones pendientes</p>
              </div>
            ) : (
              <div className="student-bell__list">
                {pendientesVisibles.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    className="student-bell__item is-unread"
                    onClick={() => void handleClickNotificacion(item)}
                  >
                    <div className="student-bell__item-header">
                      <span className="student-bell__indicator" aria-label="No leída" />
                      <strong className="student-bell__item-title">{item.titulo}</strong>
                    </div>
                    <p className="student-bell__item-body">{item.cuerpo}</p>
                    <time className="student-bell__item-time">
                      {new Date(item.creadaEn).toLocaleString([], {
                        dateStyle: 'short',
                        timeStyle: 'short',
                      })}
                    </time>
                  </button>
                ))}
              </div>
            )}
          </div>
          <footer className="student-bell__footer">
            <Link
              to="/notificaciones"
              onClick={() => setAbierta(false)}
              className="student-bell__footer-link"
            >
              Ver todas las notificaciones
            </Link>
          </footer>
        </div>
      )}
    </div>
  )
}
