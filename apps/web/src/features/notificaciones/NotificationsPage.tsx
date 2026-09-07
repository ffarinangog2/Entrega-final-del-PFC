import { useContext, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AuthContext } from '../../auth'
import { DashboardLayout } from '../../components/DashboardLayout'
import {
  listarNotificaciones,
  marcarNotificacionLeida,
  marcarTodasNotificacionesLeidas,
  type NotificacionInterna,
} from '../../services/operationalApi'
import { resolverDestinoNotificacion } from './notificationNavigation'

export function NotificationsPage() {
  const auth = useContext(AuthContext)
  const navigate = useNavigate()
  const [items, setItems] = useState<NotificacionInterna[]>([])
  const [soloNoLeidas, setSoloNoLeidas] = useState(false)
  const [error, setError] = useState('')

  const cargar = () =>
    listarNotificaciones()
      .then((data) => setItems(data ?? []))
      .catch((e) =>
        setError(e instanceof Error ? e.message : 'No fue posible cargar las notificaciones'),
      )

  useEffect(() => {
    void cargar()
  }, [])

  const handleMarcarLeida = async (id: string) => {
    try {
      const leida = await marcarNotificacionLeida(id)
      setItems((actual) =>
        actual.map((x) => (x.id === leida.id ? { ...x, leida: true } : x)),
      )
      window.dispatchEvent(new Event('notificaciones-actualizadas'))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No fue posible marcar como leída')
    }
  }

  const handleMarcarTodas = async () => {
    try {
      await marcarTodasNotificacionesLeidas()
      setItems((actual) => actual.map((x) => ({ ...x, leida: true })))
      window.dispatchEvent(new Event('notificaciones-actualizadas'))
      void cargar()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No fue posible marcar todas como leídas')
    }
  }

  const visibles = soloNoLeidas ? items.filter((x) => !x.leida) : items

  return (
    <DashboardLayout breadcrumb="Notificaciones">
      <section className="operations">
        <header className="operations__header">
          <div>
            <span className="operations__eyebrow">Bandeja personal</span>
            <h1>Notificaciones</h1>
          </div>
          <button
            type="button"
            disabled={!items.some((x) => !x.leida)}
            onClick={() => void handleMarcarTodas()}
          >
            Marcar todas como leídas
          </button>
        </header>

        {error && (
          <p role="alert" className="operations__error">
            {error}
          </p>
        )}

        <label>
          <input
            type="checkbox"
            checked={soloNoLeidas}
            onChange={(e) => setSoloNoLeidas(e.target.checked)}
          />{' '}
          Solo no leídas
        </label>

        {visibles.length === 0 ? (
          <p className="operations__empty">No hay notificaciones.</p>
        ) : (
          <div className="operations__cards">
            {visibles.map((item) => {
              const destino = resolverDestinoNotificacion(item, auth?.usuario)
              const tieneDestino = destino !== '/notificaciones'

              return (
                <article
                  key={item.id}
                  className={`operations__card ${item.leida ? 'is-read' : 'is-unread'}`}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                    <span className={`status ${item.leida ? '' : 'status--unread'}`}>
                      {item.leida ? 'Leída' : 'No leída'}
                    </span>
                    <time style={{ fontSize: '0.8rem', color: 'var(--color-text-user-muted)' }}>
                      {new Date(item.creadaEn).toLocaleString()}
                    </time>
                  </div>
                  <h2>{item.titulo}</h2>
                  <p>{item.cuerpo}</p>
                  <div className="operations__actions" style={{ marginTop: 12 }}>
                    {!item.leida && (
                      <button
                        type="button"
                        onClick={() => void handleMarcarLeida(item.id)}
                      >
                        Marcar como leída
                      </button>
                    )}
                    {tieneDestino && (
                      <button
                        type="button"
                        onClick={() => {
                          if (!item.leida) {
                            void handleMarcarLeida(item.id)
                          }
                          navigate(destino)
                        }}
                      >
                        Ver recurso
                      </button>
                    )}
                  </div>
                </article>
              )
            })}
          </div>
        )}
      </section>
    </DashboardLayout>
  )
}
