import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { DashboardLayout } from '../../components/DashboardLayout'
import { hasRole, useAuth } from '../../auth'
import {
  obtenerLaboratorios,
  obtenerMaterias,
  type Laboratorio,
  type Materia,
} from '../../services/academicoApi'
import { ApiError } from '../../services/apiClient'
import {
  obtenerReservas,
  obtenerSolicitudes,
  type Reserva,
  type SolicitudReserva,
} from './reservasApi'
import './Reservas.css'

const solicitudLabel: Record<SolicitudReserva['estado'], string> = {
  PENDIENTE: 'Pendiente',
  EN_REVISION: 'En revisión',
  PROPUESTA: 'Propuesta pendiente',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
  CANCELADA: 'Cancelada',
  EXPIRADA: 'Expirada',
}
const reservaLabel: Record<Reserva['estado'], string> = {
  PROGRAMADA: 'Programada',
  EN_CURSO: 'En curso',
  FINALIZADA: 'Finalizada',
  CANCELADA: 'Cancelada',
  NO_ASISTIDA: 'No asistida',
}

export function ReservasListPage() {
  const { usuario } = useAuth()
  const docente = hasRole(usuario, 'DOCENTE')
  const administradorPiso = hasRole(usuario, 'ADMINISTRADOR_PISO')
  const [tab, setTab] = useState<'solicitudes' | 'reservas'>('solicitudes')
  const [solicitudes, setSolicitudes] = useState<SolicitudReserva[]>([])
  const [reservas, setReservas] = useState<Reserva[]>([])
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([])
  const [materias, setMaterias] = useState<Materia[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const cargar = useCallback(async () => {
    setCargando(true)
    setError(null)
    try {
      const [solicitudesData, reservasData, laboratoriosData, materiasData] =
        await Promise.all([
          obtenerSolicitudes(),
          obtenerReservas(),
          obtenerLaboratorios(),
          obtenerMaterias(),
        ])
      setSolicitudes(solicitudesData)
      setReservas(reservasData)
      setLaboratorios(laboratoriosData)
      setMaterias(materiasData)
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 403)
        setError(
          'No tiene autorización para consultar las reservas de este piso.',
        )
      else
        setError(
          cause instanceof Error
            ? cause.message
            : 'No se pudo cargar el listado.',
        )
    } finally {
      setCargando(false)
    }
  }, [])
  useEffect(() => {
    void cargar()
  }, [cargar])

  const laboratorioPorId = useMemo(
    () => new Map(laboratorios.map((item) => [item.id, item])),
    [laboratorios],
  )
  const materiaPorId = useMemo(
    () => new Map(materias.map((item) => [item.id, item])),
    [materias],
  )
  const laboratorioNombre = (id: string) => {
    const item = laboratorioPorId.get(id)
    return item ? `${item.codigo} — ${item.nombre}` : 'Laboratorio'
  }

  const titulo = docente
    ? 'Mis solicitudes y reservas'
    : administradorPiso
      ? 'Solicitudes y reservas de mi piso'
      : 'Solicitudes y reservas'
  const contexto = docente
    ? 'Actividad asociada a su perfil docente.'
    : administradorPiso
      ? 'Operación limitada a los laboratorios de su piso.'
      : 'Vista global de la operación de reservas.'

  return (
    <DashboardLayout breadcrumb="Reservas">
      <section className="reservas-panel">
        <header className="reservas-panel__header">
          <div>
            <p className="reservas-panel__eyebrow">{contexto}</p>
            <h1>{titulo}</h1>
          </div>
          <button
            type="button"
            onClick={() => void cargar()}
            disabled={cargando}
          >
            Recargar
          </button>
        </header>
        <div className="reservas-tabs" role="tablist">
          <button
            role="tab"
            aria-selected={tab === 'solicitudes'}
            onClick={() => setTab('solicitudes')}
          >
            Solicitudes
          </button>
          <button
            role="tab"
            aria-selected={tab === 'reservas'}
            onClick={() => setTab('reservas')}
          >
            Reservas
          </button>
        </div>
        {cargando && (
          <p role="status" className="reservas-panel__message">
            Cargando información...
          </p>
        )}
        {!cargando && error && (
          <div
            role="alert"
            className="reservas-panel__message reservas-panel__message--error"
          >
            <p>{error}</p>
            <button type="button" onClick={() => void cargar()}>
              Intentar de nuevo
            </button>
          </div>
        )}
        {!cargando && !error && tab === 'solicitudes' && (
          <div className="reservas-list">
            {solicitudes.length === 0 && (
              <p className="reservas-panel__message">
                No hay solicitudes registradas.
              </p>
            )}
            {solicitudes.map((solicitud) => {
              const materia = materiaPorId.get(solicitud.materiaId)
              return (
                <article className="reserva-card" key={solicitud.id}>
                  <div className="reserva-card__heading">
                    <div>
                      <span className="reserva-card__code">
                        {materia
                          ? `${materia.codigo} — ${materia.nombre}`
                          : 'Solicitud de laboratorio'}
                      </span>
                      <h2>{solicitud.fechaReserva}</h2>
                    </div>
                    <span
                      className={`reserva-card__status reserva-card__status--${solicitud.estado.toLowerCase()}`}
                    >
                      {solicitudLabel[solicitud.estado]}
                    </span>
                  </div>
                  <dl>
                    <div>
                      <dt>Laboratorio</dt>
                      <dd>{laboratorioNombre(solicitud.laboratorioId)}</dd>
                    </div>
                    <div>
                      <dt>Horario</dt>
                      <dd>
                        {solicitud.horaInicio} – {solicitud.horaFin}
                      </dd>
                    </div>
                    <div>
                      <dt>Participantes</dt>
                      <dd>{solicitud.numeroParticipantes}</dd>
                    </div>
                  </dl>
                  <Link to={`/solicitudes/${solicitud.id}`}>Ver solicitud</Link>
                </article>
              )
            })}
          </div>
        )}
        {!cargando && !error && tab === 'reservas' && (
          <div className="reservas-list">
            {reservas.length === 0 && (
              <p className="reservas-panel__message">
                No hay reservas registradas.
              </p>
            )}
            {reservas.map((reserva) => (
              <article className="reserva-card" key={reserva.id}>
                <div className="reserva-card__heading">
                  <div>
                    <span className="reserva-card__code">
                      {reserva.codigoReserva}
                    </span>
                    <h2>{reserva.fechaReserva}</h2>
                  </div>
                  <span
                    className={`reserva-card__status reserva-card__status--${reserva.estado.toLowerCase()}`}
                  >
                    {reservaLabel[reserva.estado]}
                  </span>
                </div>
                <dl>
                  <div>
                    <dt>Laboratorio</dt>
                    <dd>{laboratorioNombre(reserva.laboratorioId)}</dd>
                  </div>
                  <div>
                    <dt>Horario</dt>
                    <dd>
                      {reserva.horaInicio} – {reserva.horaFin}
                    </dd>
                  </div>
                  <div>
                    <dt>Estado</dt>
                    <dd>{reservaLabel[reserva.estado]}</dd>
                  </div>
                </dl>
                <Link to={`/reservas/${reserva.id}`}>Ver reserva</Link>
              </article>
            ))}
          </div>
        )}
      </section>
    </DashboardLayout>
  )
}
