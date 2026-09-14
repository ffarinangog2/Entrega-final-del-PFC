import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { hasPermission, useAuth } from '../../auth'
import { DashboardLayout } from '../../components/DashboardLayout'
import { cancelarReserva, obtenerReservaPorId, ReservasApiError, type Reserva } from './reservasApi'
import './Reservas.css'

export function ReservaDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { usuario } = useAuth()
  const [reserva, setReserva] = useState<Reserva | null>(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [motivo, setMotivo] = useState('')
  const [cancelando, setCancelando] = useState(false)

  useEffect(() => {
    if (!id) { setError('El identificador de la reserva no es válido.'); setCargando(false); return }
    obtenerReservaPorId(id).then(setReserva).catch((cause: unknown) => setError(
      cause instanceof ReservasApiError && cause.status === 404
        ? 'La reserva solicitada no existe.'
        : cause instanceof Error ? cause.message : 'No se pudo cargar la reserva.',
    )).finally(() => setCargando(false))
  }, [id])

  const cancelar = async () => {
    if (!id || !motivo.trim() || cancelando || !window.confirm('¿Confirma la cancelación de esta reserva?')) return
    setCancelando(true); setError(null)
    try { setReserva(await cancelarReserva(id, motivo.trim())); setMotivo('') }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo cancelar la reserva.') }
    finally { setCancelando(false) }
  }

  return <DashboardLayout breadcrumb="Reservas / Detalle"><section className="reservas-panel reserva-detail"><Link className="reserva-detail__back" to="/reservas">← Volver al listado</Link>
    {cargando && <p role="status" className="reservas-panel__message">Cargando detalle de la reserva...</p>}
    {!cargando && error && <p role="alert" className="reservas-panel__message reservas-panel__message--error">{error}</p>}
    {!cargando && reserva && <><header className="reserva-detail__header"><div><p className="reservas-panel__eyebrow">Reserva confirmada</p><h1>{reserva.codigoReserva}</h1></div><span className={`reserva-card__status reserva-card__status--${reserva.estado.toLowerCase()}`}>{reserva.estado.replace(/_/g, ' ')}</span></header>
      <dl className="reserva-detail__data"><div><dt>Solicitud</dt><dd>{reserva.solicitudId}</dd></div><div><dt>Laboratorio</dt><dd>{reserva.laboratorioId}</dd></div><div><dt>Fecha</dt><dd>{reserva.fechaReserva}</dd></div><div><dt>Horario</dt><dd>{reserva.horaInicio} – {reserva.horaFin}</dd></div></dl>
      {reserva.estado === 'PROGRAMADA' && hasPermission(usuario, 'RESERVA_CANCELAR') && <div className="cancel-box"><label>Motivo de cancelación<textarea value={motivo} maxLength={2000} onChange={(event) => setMotivo(event.target.value)} /></label><button type="button" disabled={cancelando || !motivo.trim()} onClick={() => void cancelar()}>{cancelando ? 'Cancelando...' : 'Cancelar reserva'}</button></div>}</>}
  </section></DashboardLayout>
}
