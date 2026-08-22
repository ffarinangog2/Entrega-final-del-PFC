import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { DashboardLayout } from '../../components/DashboardLayout'
import { obtenerReservas, type Reserva } from './reservasApi'
import './Reservas.css'
export function ReservasListPage() {
  const [reservas, setReservas] = useState<Reserva[]>([]); const [cargando, setCargando] = useState(true); const [error, setError] = useState<string | null>(null)
  const cargar = useCallback(async () => { setCargando(true); setError(null); try { setReservas(await obtenerReservas()) } catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo cargar el listado.') } finally { setCargando(false) } }, [])
  useEffect(() => { void cargar() }, [cargar])
  return <DashboardLayout breadcrumb="Reservas"><section className="reservas-panel">
    <header className="reservas-panel__header"><div><p className="reservas-panel__eyebrow">Gestión académica</p><h1>Reservas</h1></div><button type="button" onClick={() => void cargar()} disabled={cargando}>Recargar</button></header>
    {cargando && <p role="status" className="reservas-panel__message">Cargando reservas...</p>}
    {!cargando && error && <div role="alert" className="reservas-panel__message reservas-panel__message--error"><p>{error}</p><button type="button" onClick={() => void cargar()}>Intentar de nuevo</button></div>}
    {!cargando && !error && reservas.length === 0 && <p className="reservas-panel__message">No hay reservas registradas.</p>}
    {!cargando && !error && reservas.length > 0 && <div className="reservas-list">{reservas.map((reserva) => <article className="reserva-card" key={reserva.id}>
      <div className="reserva-card__heading"><div><span className="reserva-card__code">{reserva.codigoReserva}</span><h2>{reserva.fechaReserva}</h2></div><span className={`reserva-card__status reserva-card__status--${reserva.estado.toLowerCase()}`}>{reserva.estado.replace(/_/g, ' ')}</span></div>
      <dl><div><dt>Laboratorio</dt><dd>{reserva.laboratorioId}</dd></div><div><dt>Horario</dt><dd>{reserva.horaInicio} – {reserva.horaFin}</dd></div><div><dt>Responsable</dt><dd>{reserva.responsableId}</dd></div></dl>
      <Link to={`/reservas/${reserva.id}`}>Ver detalle</Link>
    </article>)}</div>}
  </section></DashboardLayout>
}
