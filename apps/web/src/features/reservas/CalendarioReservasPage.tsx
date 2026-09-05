import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { hasRole, useAuth } from '../../auth'
import { DashboardLayout } from '../../components/DashboardLayout'
import {
  obtenerLaboratorios,
  obtenerPisos,
  type Laboratorio,
  type Piso,
} from '../../services/academicoApi'
import { obtenerCalendario, type Reserva } from './reservasApi'
import './Reservas.css'
import { useAcademicPeriod } from '../../academicPeriod'
import { obtenerDisponibilidadPlanificacion } from '../../services/operationalApi'

function inicioSemana(fecha: Date) {
  const copia = new Date(fecha)
  const dia = copia.getDay() || 7
  copia.setDate(copia.getDate() - dia + 1)
  copia.setHours(0, 0, 0, 0)
  return copia
}
function iso(fecha: Date) {
  return fecha.toISOString().slice(0, 10)
}

export function CalendarioReservasPage() {
  const { periodoSeleccionado } = useAcademicPeriod()
  const { usuario } = useAuth()
  const coordinador = hasRole(usuario, 'COORDINADOR')
  const administrador = hasRole(usuario, 'ADMINISTRADOR')
  const [inicio, setInicio] = useState(() => inicioSemana(new Date()))
  const [reservas, setReservas] = useState<Reserva[]>([])
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([])
  const [pisos, setPisos] = useState<Piso[]>([])
  const [filtros, setFiltros] = useState({ piso: '', laboratorio: '', fecha: iso(new Date()), horaInicio: '07:30', horaFin: '08:30', capacidad: 0, estado: '' })
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [ocupadosPlan, setOcupadosPlan] = useState<string[]>([])
  const fin = new Date(inicio)
  fin.setDate(fin.getDate() + 6)

  useEffect(() => {
    const hasta = new Date(inicio)
    hasta.setDate(hasta.getDate() + 6)
    setCargando(true)
    setError(null)
    obtenerCalendario(iso(inicio), iso(hasta))
      .then(setReservas)
      .catch((cause) =>
        setError(
          cause instanceof Error
            ? cause.message
            : 'No se pudo cargar el calendario.',
        ),
      )
      .finally(() => setCargando(false))
  }, [inicio, coordinador])
  useEffect(() => {
    obtenerLaboratorios()
      .then(setLaboratorios)
      .catch(() => setLaboratorios([]))
  }, [])
  useEffect(() => {
    if (administrador || coordinador) void obtenerPisos().then(setPisos).catch(() => setPisos([]))
  }, [administrador, coordinador])
  useEffect(() => {
    if (!coordinador || !periodoSeleccionado) return
    const dia = new Date(`${filtros.fecha}T12:00:00`).toLocaleDateString('es-EC', { weekday: 'long' }).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase()
    obtenerDisponibilidadPlanificacion({ periodoId: periodoSeleccionado.id, dia, horaInicio: filtros.horaInicio, horaFin: filtros.horaFin }).then((data) => setOcupadosPlan(data.laboratoriosOcupados)).catch(() => setOcupadosPlan([]))
  }, [coordinador, periodoSeleccionado, filtros.fecha, filtros.horaInicio, filtros.horaFin])
  const mover = (cantidad: number) =>
    setInicio((actual) => {
      const siguiente = new Date(actual)
      siguiente.setDate(siguiente.getDate() + cantidad)
      return siguiente
    })
  const dias = Array.from({ length: 7 }, (_, indice) => {
    const fecha = new Date(inicio)
    fecha.setDate(fecha.getDate() + indice)
    return fecha
  })

  return (
    <DashboardLayout
      breadcrumb={
        coordinador ? 'Disponibilidad de laboratorios' : 'Reservas / Calendario'
      }
    >
      <section className="reservas-panel">
        <header className="reservas-panel__header">
          <div>
            <p className="reservas-panel__eyebrow">
              {coordinador ? 'Consulta para planificación' : 'Vista semanal'}
            </p>
            <h1>
              {iso(inicio)} — {iso(fin)}
            </h1>
          </div>
          <div>
            <button onClick={() => mover(-7)}>Semana anterior</button>
            <button onClick={() => setInicio(inicioSemana(new Date()))}>
              Actual
            </button>
            <button onClick={() => mover(7)}>Semana siguiente</button>
          </div>
        </header>
        {(administrador || coordinador) && <div className="reservas-filters">
          <label>Piso<select value={filtros.piso} onChange={(e) => setFiltros({ ...filtros, piso: e.target.value, laboratorio: '' })}><option value="">Todos</option>{pisos.map((piso) => <option key={piso.id} value={piso.id}>Piso {piso.numero}</option>)}</select></label>
          <label>Laboratorio<select value={filtros.laboratorio} onChange={(e) => setFiltros({ ...filtros, laboratorio: e.target.value })}><option value="">Todos</option>{laboratorios.filter((lab) => !filtros.piso || lab.pisoId === filtros.piso).map((lab) => <option key={lab.id} value={lab.id}>{lab.codigo} — {lab.nombre}</option>)}</select></label>
          {coordinador && <><label>Fecha<input type="date" value={filtros.fecha} onChange={(e) => setFiltros({ ...filtros, fecha: e.target.value })} /></label><label>Desde<input type="time" value={filtros.horaInicio} onChange={(e) => setFiltros({ ...filtros, horaInicio: e.target.value })} /></label><label>Hasta<input type="time" value={filtros.horaFin} onChange={(e) => setFiltros({ ...filtros, horaFin: e.target.value })} /></label><label>Capacidad mínima<input type="number" min="0" value={filtros.capacidad} onChange={(e) => setFiltros({ ...filtros, capacidad: Number(e.target.value) })} /></label><label>Estado<select value={filtros.estado} onChange={(e) => setFiltros({ ...filtros, estado: e.target.value })}><option value="">Todos</option><option value="DISPONIBLE">Disponible</option><option value="OCUPADO">Ocupado</option><option value="MANTENIMIENTO">Mantenimiento</option></select></label></>}
        </div>}
        {coordinador ? (
          <div className="reservas-list reservas-list--availability">
            {laboratorios.length === 0 ? (
              <p>No hay laboratorios disponibles para consultar.</p>
            ) : (
              laboratorios
                .filter((lab) => !filtros.piso || lab.pisoId === filtros.piso)
                .filter((lab) => !filtros.laboratorio || lab.id === filtros.laboratorio)
                .filter((lab) => lab.capacidad >= filtros.capacidad)
                .filter((lab) => { const ocupado = ocupadosPlan.includes(lab.id) || reservas.some((r) => r.laboratorioId === lab.id && r.fechaReserva === filtros.fecha && r.horaInicio < filtros.horaFin && r.horaFin > filtros.horaInicio); const estado = lab.estado !== 'DISPONIBLE' ? lab.estado : ocupado ? 'OCUPADO' : 'DISPONIBLE'; return !filtros.estado || filtros.estado === estado })
                .map((lab) => (
                <article className="reserva-card reserva-card--availability" key={lab.id}>
                  <h2>
                    {lab.codigo} — {lab.nombre}
                  </h2>
                  <p>{(lab.estado !== 'DISPONIBLE' ? lab.estado : ocupadosPlan.includes(lab.id) || reservas.some((reserva) => reserva.laboratorioId === lab.id && reserva.fechaReserva === filtros.fecha && reserva.horaInicio < filtros.horaFin && reserva.horaFin > filtros.horaInicio) ? 'OCUPADO' : 'DISPONIBLE').replace(/_/g, ' ')}</p>
                  <p>Capacidad: {lab.capacidad}</p>
                </article>
              ))
            )}
          </div>
        ) : (
          <>
            {cargando && (
              <p role="status" className="reservas-panel__message">
                Cargando calendario...
              </p>
            )}
            {!cargando && error && (
              <p
                role="alert"
                className="reservas-panel__message reservas-panel__message--error"
              >
                {error}
              </p>
            )}
            {!cargando && !error && reservas.length === 0 && (
              <p className="reservas-panel__message">
                No hay reservas esta semana.
              </p>
            )}
            {!cargando && !error && reservas.length > 0 && (
              <div className="week-grid">
                {dias.map((dia) => (
                  <section key={iso(dia)}>
                    <h2>
                      {dia.toLocaleDateString('es-EC', {
                        weekday: 'short',
                        day: '2-digit',
                      })}
                    </h2>
                    {reservas
                      .filter((reserva) => !filtros.laboratorio || reserva.laboratorioId === filtros.laboratorio)
                      .filter((reserva) => !filtros.piso || laboratorios.find((lab) => lab.id === reserva.laboratorioId)?.pisoId === filtros.piso)
                      .filter((reserva) => reserva.fechaReserva === iso(dia))
                      .map((reserva) => {
                        const lab = laboratorios.find(
                          (item) => item.id === reserva.laboratorioId,
                        )
                        return (
                          <Link key={reserva.id} to={`/reservas/${reserva.id}`}>
                            <strong>
                              {reserva.horaInicio.slice(0, 5)}{' '}
                              {reserva.codigoReserva}
                            </strong>
                            <span>
                              {lab
                                ? `${lab.codigo} — ${lab.nombre}`
                                : 'Laboratorio'}
                            </span>
                            <span>{reserva.estado.replace(/_/g, ' ')}</span>
                          </Link>
                        )
                      })}
                  </section>
                ))}
              </div>
            )}
          </>
        )}
      </section>
    </DashboardLayout>
  )
}
