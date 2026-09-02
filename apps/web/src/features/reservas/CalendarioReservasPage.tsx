import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { hasRole, useAuth } from '../../auth'
import { DashboardLayout } from '../../components/DashboardLayout'
import {
  obtenerLaboratorios,
  type Laboratorio,
} from '../../services/academicoApi'
import { obtenerCalendario, type Reserva } from './reservasApi'
import './Reservas.css'

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
  const { usuario } = useAuth()
  const coordinador = hasRole(usuario, 'COORDINADOR')
  const [inicio, setInicio] = useState(() => inicioSemana(new Date()))
  const [reservas, setReservas] = useState<Reserva[]>([])
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const fin = new Date(inicio)
  fin.setDate(fin.getDate() + 6)

  useEffect(() => {
    if (coordinador) {
      setCargando(false)
      return
    }
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
        {coordinador ? (
          <div className="reservas-list">
            {laboratorios.length === 0 ? (
              <p>No hay laboratorios disponibles para consultar.</p>
            ) : (
              laboratorios.map((lab) => (
                <article className="reserva-card" key={lab.id}>
                  <h2>
                    {lab.codigo} — {lab.nombre}
                  </h2>
                  <p>{lab.estado.replace(/_/g, ' ')}</p>
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
