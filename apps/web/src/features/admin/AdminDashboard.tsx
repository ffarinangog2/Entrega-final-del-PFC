import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  obtenerDocentes,
  obtenerLaboratorios,
} from '../../services/academicoApi'
import {
  listarIncidentes,
  listarPlanificaciones,
} from '../../services/operationalApi'
import { listarPerfiles } from '../../services/usuariosApi'
import { obtenerReservas, obtenerSolicitudes } from '../reservas/reservasApi'

interface Resumen {
  usuarios: number
  usuariosActivos: number
  docentes: number
  laboratorios: number
  disponibles: number
  ocupados: number
  fueraServicio: number
  reservasHoy: number
  solicitudesPendientes: number
  incidentesActivos: number
  planificaciones: Record<string, number>
}

export function AdminDashboard() {
  const [resumen, setResumen] = useState<Resumen | null>(null)
  const [error, setError] = useState('')
  const hoy = useMemo(() => new Date().toISOString().slice(0, 10), [])

  useEffect(() => {
    let activo = true
    Promise.all([
      listarPerfiles(),
      obtenerDocentes(),
      obtenerLaboratorios(),
      obtenerReservas(),
      obtenerSolicitudes(),
      listarIncidentes(),
      listarPlanificaciones(),
    ])
      .then(
        ([
          usuarios,
          docentes,
          laboratorios,
          reservas,
          solicitudes,
          incidentes,
          planes,
        ]) => {
          if (!activo) return
          setResumen({
            usuarios: usuarios.length,
            usuariosActivos: usuarios.filter((item) => item.activo).length,
            docentes: docentes.filter((item) => item.activo).length,
            laboratorios: laboratorios.length,
            disponibles: laboratorios.filter(
              (item) => item.estado === 'DISPONIBLE',
            ).length,
            ocupados: laboratorios.filter((item) => item.estado === 'OCUPADO')
              .length,
            fueraServicio: laboratorios.filter((item) =>
              ['MANTENIMIENTO', 'INACTIVO'].includes(item.estado),
            ).length,
            reservasHoy: reservas.filter((item) => item.fechaReserva === hoy)
              .length,
            solicitudesPendientes: solicitudes.filter((item) =>
              ['PENDIENTE', 'EN_REVISION'].includes(item.estado),
            ).length,
            incidentesActivos: incidentes.filter(
              (item) => item.estado !== 'RESUELTO',
            ).length,
            planificaciones: planes.reduce<Record<string, number>>(
              (total, item) => {
                total[item.estado] = (total[item.estado] ?? 0) + 1
                return total
              },
              {},
            ),
          })
        },
      )
      .catch(
        () =>
          activo &&
          setError(
            'No fue posible cargar el resumen global. Puede acceder a cada módulo desde el menú.',
          ),
      )
    return () => {
      activo = false
    }
  }, [hoy])

  return (
    <section className="admin-overview" aria-labelledby="admin-title">
      <header>
        <p>Supervisión institucional</p>
        <h1 id="admin-title">Sistema SCLI</h1>
      </header>
      {!resumen && !error && <p role="status">Cargando resumen global...</p>}
      {error && <p role="alert">{error}</p>}
      {resumen && (
        <>
          <div className="admin-overview__metrics">
            <Metric
              label="Usuarios activos"
              value={resumen.usuariosActivos}
              detail={`${resumen.usuarios} perfiles registrados`}
            />
            <Metric label="Docentes activos" value={resumen.docentes} />
            <Metric
              label="Laboratorios"
              value={resumen.laboratorios}
              detail={`${resumen.disponibles} disponibles · ${resumen.ocupados} ocupados · ${resumen.fueraServicio} fuera de servicio`}
            />
            <Metric label="Reservas de hoy" value={resumen.reservasHoy} />
            <Metric
              label="Solicitudes pendientes"
              value={resumen.solicitudesPendientes}
            />
            <Metric
              label="Incidentes activos"
              value={resumen.incidentesActivos}
            />
          </div>
          <section>
            <h2>Planificación global</h2>
            <p>
              {Object.entries(resumen.planificaciones)
                .map(
                  ([estado, cantidad]) =>
                `${estado.replace(/_/g, ' ')}: ${cantidad}`,
                )
                .join(' · ') || 'Sin planificaciones registradas.'}
            </p>
          </section>
        </>
      )}
      <nav className="role-home__links" aria-label="Accesos de supervisión">
        <Link to="/usuarios">Gestionar usuarios</Link>
        <Link to="/main">Laboratorios y monitoreo</Link>
        <Link to="/planificacion">Planificación global</Link>
        <Link to="/reservas">Reservas</Link>
        <Link to="/incidentes">Incidentes</Link>
        <Link to="/reservas/calendario">Ocupación</Link>
      </nav>
    </section>
  )
}

function Metric({
  label,
  value,
  detail,
}: {
  label: string
  value: number
  detail?: string
}) {
  return (
    <article>
      <span>{label}</span>
      <strong>{value}</strong>
      {detail && <small>{detail}</small>}
    </article>
  )
}
