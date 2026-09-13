import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { DashboardLayout } from '../../components/DashboardLayout'
import { hasRole, useAuth } from '../../auth'
import {
  obtenerLaboratorios,
  obtenerMaterias,
  obtenerPisos,
  type Laboratorio,
  type Materia,
  type Piso,
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
  const administrador = hasRole(usuario, 'ADMINISTRADOR')
  const [tab, setTab] = useState<'solicitudes' | 'reservas'>('solicitudes')
  const [solicitudes, setSolicitudes] = useState<SolicitudReserva[]>([])
  const [reservas, setReservas] = useState<Reserva[]>([])
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([])
  const [materias, setMaterias] = useState<Materia[]>([])
  const [pisosCatalogo, setPisosCatalogo] = useState<Piso[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filtros, setFiltros] = useState({ piso: '', laboratorio: '', estado: '', fecha: '' })

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
  useEffect(() => {
    if (administrador) void obtenerPisos().then(setPisosCatalogo).catch(() => setPisosCatalogo([]))
  }, [administrador])

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
  const coincide = (laboratorioId: string, estadoItem: string, fecha: string) => {
    const lab = laboratorioPorId.get(laboratorioId)
    return (!filtros.piso || lab?.pisoId === filtros.piso)
      && (!filtros.laboratorio || laboratorioId === filtros.laboratorio)
      && (!filtros.estado || estadoItem === filtros.estado)
      && (!filtros.fecha || fecha === filtros.fecha)
  }
  const solicitudesVisibles = solicitudes.filter((item) => coincide(item.laboratorioId, item.estado, item.fechaReserva))
  const reservasVisibles = reservas.filter((item) => coincide(item.laboratorioId, item.estado, item.fechaReserva))
  const pisos = [...new Set(laboratorios.map((item) => item.pisoId))]

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
        {administrador && <div className="reservas-filters">
          <label>Piso<select value={filtros.piso} onChange={(e) => setFiltros({ ...filtros, piso: e.target.value })}><option value="">Todos</option>{pisos.map((id) => <option key={id} value={id}>Piso {pisosCatalogo.find((piso) => piso.id === id)?.numero ?? 'sin nombre'}</option>)}</select></label>
          <label>Laboratorio<select value={filtros.laboratorio} onChange={(e) => setFiltros({ ...filtros, laboratorio: e.target.value })}><option value="">Todos</option>{laboratorios.filter((lab) => !filtros.piso || lab.pisoId === filtros.piso).map((lab) => <option key={lab.id} value={lab.id}>{lab.codigo} — {lab.nombre}</option>)}</select></label>
          <label>Estado<select value={filtros.estado} onChange={(e) => setFiltros({ ...filtros, estado: e.target.value })}><option value="">Todos</option>{(tab === 'solicitudes' ? Object.keys(solicitudLabel) : Object.keys(reservaLabel)).map((estado) => <option key={estado}>{estado}</option>)}</select></label>
          <label>Fecha<input type="date" value={filtros.fecha} onChange={(e) => setFiltros({ ...filtros, fecha: e.target.value })} /></label>
        </div>}
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
            {solicitudesVisibles.length === 0 && (
              <p className="reservas-panel__message">
                No hay solicitudes registradas.
              </p>
            )}
            {solicitudesVisibles.map((solicitud) => {
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
            {reservasVisibles.length === 0 && (
              <p className="reservas-panel__message">
                No hay reservas registradas.
              </p>
            )}
            {reservasVisibles.map((reserva) => (
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
