import { useCallback, useContext, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthContext } from '../../auth'
import { obtenerLaboratorios, obtenerMaterias } from '../../services/academicoApi'
import {
  listarNotificaciones,
  marcarNotificacionLeida,
  marcarTodasNotificacionesLeidas,
  listarSesionesAbiertas,
  obtenerMiHorario,
  type NotificacionInterna,
  type Planificacion,
  type SesionAsistencia,
} from '../../services/operationalApi'
import { destinoNotificacion } from '../notificaciones/notificationNavigation'

export function StudentNotificationBell({ asistencia = true }: { asistencia?: boolean }) {
  const navigate = useNavigate()
  const auth = useContext(AuthContext)
  const authenticated = auth ? auth.isAuthenticated : true
  const [sesiones, setSesiones] = useState<SesionAsistencia[]>([])
  const [abierta, setAbierta] = useState(false)
  const [horario, setHorario] = useState<Planificacion[]>([])
  const [materias, setMaterias] = useState<Map<string, string>>(new Map())
  const [labs, setLabs] = useState<Map<string, string>>(new Map())
  const [notificaciones, setNotificaciones] = useState<NotificacionInterna[]>([])

  const refrescarNotificaciones = useCallback(() => {
    if (!authenticated) return
    void Promise.resolve(listarNotificaciones())
      .then((value) => setNotificaciones(value ?? []))
      .catch(() => undefined)
    if (asistencia) {
      void Promise.resolve(listarSesionesAbiertas())
        .then((items) => setSesiones(items ?? []))
        .catch(() => undefined)
    }
  }, [asistencia, authenticated])

  useEffect(() => {
    let activo = true
    if (!authenticated) {
      setNotificaciones([])
      setSesiones([])
      setHorario([])
      return () => {
        activo = false
      }
    }
    Promise.all([
      Promise.resolve()
        .then(() => listarNotificaciones())
        .then((value) => value ?? [])
        .catch(() => []),
      asistencia
        ? Promise.resolve()
            .then(() => listarSesionesAbiertas())
            .then((value) => value ?? [])
            .catch(() => [])
        : Promise.resolve([]),
      asistencia
        ? Promise.resolve()
            .then(() => obtenerMiHorario())
            .then((value) => value ?? [])
            .catch(() => [])
        : Promise.resolve([]),
      asistencia
        ? Promise.resolve()
            .then(() => obtenerMaterias())
            .then((value) => value ?? [])
            .catch(() => [])
        : Promise.resolve([]),
      asistencia
        ? Promise.resolve()
            .then(() => obtenerLaboratorios())
            .then((value) => value ?? [])
            .catch(() => [])
        : Promise.resolve([]),
    ])
      .then(([avisos, items, bloques, materiasData, labsData]) => {
        if (!activo) return
        setNotificaciones(avisos)
        setSesiones(items)
        setHorario(bloques)
        setMaterias(new Map(materiasData.map((item) => [item.id, item.nombre])))
        setLabs(new Map(labsData.map((item) => [item.id, item.codigo])))
      })
      .catch(() => undefined)
    return () => {
      activo = false
    }
  }, [asistencia, authenticated])

  useEffect(() => {
    if (!authenticated) return undefined
    const alRecuperarFoco = () => refrescarNotificaciones()
    window.addEventListener('focus', alRecuperarFoco)
    const polling = window.setInterval(refrescarNotificaciones, 45_000)
    return () => {
      window.removeEventListener('focus', alRecuperarFoco)
      window.clearInterval(polling)
    }
  }, [authenticated, refrescarNotificaciones])

  async function abrirItem(item: NotificacionInterna) {
    if (!item.leida) {
      const leida = await marcarNotificacionLeida(item.id)
      setNotificaciones((actuales) =>
        actuales.map((actual) => (actual.id === leida.id ? leida : actual)),
      )
    }
    setAbierta(false)
    navigate(destinoNotificacion(item))
  }

  async function leerTodas() {
    await marcarTodasNotificacionesLeidas().catch(() => undefined)
    setNotificaciones((actuales) => actuales.map((x) => ({ ...x, leida: true })))
  }

  const noLeidas = notificaciones.filter((item) => !item.leida).length
  const totalPendientes = noLeidas + (asistencia ? sesiones.length : 0)

  return (
    <div className="student-bell">
      <button
        aria-label={`Notificaciones: ${totalPendientes} pendientes`}
        aria-expanded={abierta}
        onClick={() => setAbierta((value) => !value)}
      >
        🔔
        {totalPendientes > 0 && <span>{totalPendientes}</span>}
      </button>
      {abierta && (
        <div className="student-bell__panel" role="dialog" aria-label="Notificaciones">
          <header>
            <div>
              <strong>Notificaciones</strong>
              <small>{noLeidas} sin leer</small>
            </div>
            <button disabled={!noLeidas} onClick={() => void leerTodas()}>
              Marcar todas como leídas
            </button>
          </header>
          <div className="student-bell__items">
            {notificaciones.length === 0 && sesiones.length === 0 ? (
              <p>No hay notificaciones.</p>
            ) : (
              <>
                {notificaciones.slice(0, 5).map((item) => (
                  <button
                    key={item.id}
                    className={item.leida ? '' : 'is-unread'}
                    onClick={() => void abrirItem(item)}
                  >
                    <span className="student-bell__icon" aria-hidden="true">
                      {icono(item.tipo)}
                    </span>
                    <span>
                      <strong>{item.titulo}</strong>
                      <small>{item.cuerpo}</small>
                      <time>{new Date(item.creadaEn).toLocaleString()}</time>
                    </span>
                  </button>
                ))}
                {sesiones.slice(0, 5).map((sesion) => {
                  const bloque = horario.find((item) => item.id === sesion.bloqueId)
                  return (
                    <Link key={sesion.id} to="/asistencia" onClick={() => setAbierta(false)}>
                      Asistencia disponible · {materias.get(bloque?.materiaId ?? '') ?? 'Actividad de laboratorio'} ·{' '}
                      {labs.get(bloque?.laboratorioId ?? '') ?? 'Laboratorio'} · hasta{' '}
                      {new Date(sesion.expiraEn).toLocaleTimeString([], {
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </Link>
                  )
                })}
              </>
            )}
          </div>
          <footer>
            <Link to="/notificaciones" onClick={() => setAbierta(false)}>
              Ver todas las notificaciones
            </Link>
          </footer>
        </div>
      )}
    </div>
  )
}

function icono(tipo: string | null) {
  if (tipo?.includes('RETIRO')) return '↩'
  if (tipo?.includes('USO') || tipo?.includes('ASISTENCIA')) return '✓'
  if (tipo?.includes('PLANIFICACION') || tipo?.includes('HORARIO')) return '▦'
  return 'i'
}
