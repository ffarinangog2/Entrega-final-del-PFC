import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { DashboardLayout } from '../../components/DashboardLayout'
import {
  listarNotificaciones,
  listarNotificacionesPaginadas,
  marcarNotificacionLeida,
  marcarTodasNotificacionesLeidas,
  type NotificacionInterna,
} from '../../services/operationalApi'
import { destinoNotificacion } from './notificationNavigation'

export function NotificationsPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<NotificacionInterna[]>([])
  const [pagina, setPagina] = useState(0)
  const [total, setTotal] = useState(1)
  const [soloNoLeidas, setSoloNoLeidas] = useState(false)
  const [error, setError] = useState('')

  const cargar = useCallback(() => {
    let handled = false
    try {
      if (typeof listarNotificacionesPaginadas === 'function') {
        const promise = listarNotificacionesPaginadas(pagina, 10)
        if (promise && typeof promise.then === 'function') {
          handled = true
          promise
            .then((r) => {
              if (r && Array.isArray(r.content)) {
                setItems(r.content)
                setTotal(r.totalPages || 1)
              } else {
                return listarNotificaciones().then((data) => {
                  setItems(data ?? [])
                  setTotal(1)
                })
              }
            })
            .catch(() => {
              listarNotificaciones()
                .then((data) => {
                  setItems(data ?? [])
                  setTotal(1)
                })
                .catch((e) =>
                  setError(
                    e instanceof Error
                      ? e.message
                      : 'No fue posible cargar las notificaciones',
                  ),
                )
            })
        }
      }
    } catch {
      handled = false
    }

    if (!handled) {
      listarNotificaciones()
        .then((data) => {
          setItems(data ?? [])
          setTotal(1)
        })
        .catch((e) =>
          setError(
            e instanceof Error
              ? e.message
              : 'No fue posible cargar las notificaciones',
          ),
        )
    }
  }, [pagina])

  useEffect(() => {
    void cargar()
  }, [cargar])

  async function abrir(item: NotificacionInterna) {
    if (!item.leida) {
      const leida = await marcarNotificacionLeida(item.id)
      setItems((actual) =>
        actual.map((x) => (x.id === leida.id ? leida : x)),
      )
    }
    navigate(destinoNotificacion(item))
  }

  async function marcarUna(id: string) {
    const leida = await marcarNotificacionLeida(id)
    setItems((actual) => actual.map((x) => (x.id === leida.id ? leida : x)))
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
            disabled={!items.some((x) => !x.leida)}
            onClick={() => void marcarTodasNotificacionesLeidas().then(cargar)}
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
            {visibles.map((item) => (
              <article
                key={item.id}
                className={`operations__card notification-card ${
                  item.leida ? '' : 'is-unread'
                }`}
              >
                <div
                  onClick={() => void abrir(item)}
                  style={{ cursor: 'pointer' }}
                >
                  <strong>{item.titulo}</strong>
                  <p>{item.cuerpo}</p>
                  <time>{new Date(item.creadaEn).toLocaleString()}</time>
                </div>
                {!item.leida && (
                  <button onClick={() => void marcarUna(item.id)}>
                    Marcar como leída
                  </button>
                )}
              </article>
            ))}
          </div>
        )}
        <nav className="operations__actions" aria-label="Paginación">
          <button disabled={pagina === 0} onClick={() => setPagina((x) => x - 1)}>
            Anterior
          </button>
          <span>
            Página {pagina + 1} de {Math.max(1, total)}
          </span>
          <button
            disabled={pagina + 1 >= total}
            onClick={() => setPagina((x) => x + 1)}
          >
            Siguiente
          </button>
        </nav>
      </section>
    </DashboardLayout>
  )
}
