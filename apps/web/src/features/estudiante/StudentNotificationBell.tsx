import { useContext, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { AuthContext } from '../../auth'
import { obtenerLaboratorios, obtenerMaterias } from '../../services/academicoApi'
import {
  listarNotificaciones,
  marcarNotificacionLeida,
  listarSesionesAbiertas,
  obtenerMiHorario,
  type NotificacionInterna,
  type Planificacion,
  type SesionAsistencia,
} from '../../services/operationalApi'

export function StudentNotificationBell({ asistencia = true }: { asistencia?: boolean }) {
  const auth = useContext(AuthContext)
  const authenticated = auth ? auth.isAuthenticated : true
  const [sesiones, setSesiones] = useState<SesionAsistencia[]>([])
  const [abierta, setAbierta] = useState(false)
  const [horario, setHorario] = useState<Planificacion[]>([])
  const [materias, setMaterias] = useState<Map<string, string>>(new Map())
  const [labs, setLabs] = useState<Map<string, string>>(new Map())
  const [notificaciones, setNotificaciones] = useState<NotificacionInterna[]>([])

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

  return (
    <div className="student-bell">
      <button
        aria-label={`Notificaciones: ${notificaciones.filter((item) => !item.leida).length + sesiones.length} pendientes`}
        onClick={() => setAbierta((value) => !value)}
      >
        🔔
        {notificaciones.filter((item) => !item.leida).length + sesiones.length > 0 && (
          <span>{notificaciones.filter((item) => !item.leida).length + sesiones.length}</span>
        )}
      </button>
      {abierta && (
        <div role="dialog" aria-label="Notificaciones">
          {notificaciones.length === 0 && sesiones.length === 0 ? (
            <p>No hay notificaciones.</p>
          ) : (
            <>
              {notificaciones.slice(0, 5).map((item) => (
                <button
                  key={item.id}
                  className={item.leida ? '' : 'is-unread'}
                  onClick={() =>
                    void marcarNotificacionLeida(item.id).then((leida) =>
                      setNotificaciones((actuales) =>
                        actuales.map((actual) => (actual.id === leida.id ? leida : actual)),
                      ),
                    )
                  }
                >
                  <strong>{item.titulo}</strong>
                  <span>{item.cuerpo}</span>
                </button>
              ))}
              {sesiones.slice(0, 5).map((sesion) => {
                const bloque = horario.find((item) => item.id === sesion.bloqueId)
                return (
                  <Link key={sesion.id} to="/asistencia">
                    Asistencia disponible · {materias.get(bloque?.materiaId ?? '') ?? 'Actividad de laboratorio'} ·{' '}
                    {labs.get(bloque?.laboratorioId ?? '') ?? 'Laboratorio'} · hasta{' '}
                    {new Date(sesion.expiraEn).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </Link>
                )
              })}
            </>
          )}
          <Link to="/notificaciones">Ver todas las notificaciones</Link>
        </div>
      )}
    </div>
  )
}
