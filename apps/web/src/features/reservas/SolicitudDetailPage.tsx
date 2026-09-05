import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { hasPermission, useAuth } from '../../auth'
import { DashboardLayout } from '../../components/DashboardLayout'
import { obtenerLaboratorios, obtenerMaterias, type Laboratorio, type Materia } from '../../services/academicoApi'
import {
  aprobarSolicitud, cancelarSolicitud, obtenerHistorialSolicitud, obtenerSolicitudPorId,
  ponerEnRevision, proponerAlternativa, rechazarSolicitud, responderPropuesta,
  type HistorialSolicitud, type SolicitudReserva,
} from './reservasApi'
import './Reservas.css'

const estadosCancelables = new Set(['PENDIENTE', 'EN_REVISION', 'PROPUESTA', 'APROBADA'])

export function SolicitudDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { usuario } = useAuth()
  const [solicitud, setSolicitud] = useState<SolicitudReserva | null>(null)
  const [historial, setHistorial] = useState<HistorialSolicitud[]>([])
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([])
  const [materias, setMaterias] = useState<Materia[]>([])
  const [error, setError] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)
  const [comentario, setComentario] = useState('')
  const [propuesta, setPropuesta] = useState({ laboratorioId: '', fecha: '', horaInicio: '', horaFin: '', observacion: '' })
  const approvalKey = useRef(crypto.randomUUID())

  const cargar = useCallback(async () => {
    if (!id) return
    try {
      const [solicitudData, historialData, laboratoriosData, materiasData] = await Promise.all([
        obtenerSolicitudPorId(id), obtenerHistorialSolicitud(id), obtenerLaboratorios(), obtenerMaterias(),
      ])
      setSolicitud(solicitudData); setHistorial(historialData); setLaboratorios(laboratoriosData); setMaterias(materiasData)
      setPropuesta((current) => ({ ...current, laboratorioId: solicitudData.laboratorioId, fecha: solicitudData.fechaReserva, horaInicio: solicitudData.horaInicio.slice(0, 5), horaFin: solicitudData.horaFin.slice(0, 5) }))
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo cargar la solicitud.') }
  }, [id])
  useEffect(() => { void cargar() }, [cargar])

  const labMap = useMemo(() => new Map(laboratorios.map((item) => [item.id, item])), [laboratorios])
  const materiaMap = useMemo(() => new Map(materias.map((item) => [item.id, item])), [materias])
  const propietario = solicitud?.solicitanteId === usuario?.perfilId
  const puedeCrear = hasPermission(usuario, 'SOLICITUD_CREAR')
  const puedeAprobar = hasPermission(usuario, 'SOLICITUD_APROBAR')
  const puedeRechazar = hasPermission(usuario, 'SOLICITUD_RECHAZAR')

  const ejecutar = async (operation: () => Promise<unknown>) => {
    if (ocupado) return
    setOcupado(true); setError(null)
    try { await operation(); setComentario(''); await cargar() }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo completar la acción.') }
    finally { setOcupado(false) }
  }

  if (!solicitud) return <DashboardLayout breadcrumb="Solicitudes / Detalle"><section className="reservas-panel">{error ? <p role="alert">{error}</p> : <p role="status">Cargando solicitud...</p>}</section></DashboardLayout>
  const laboratorio = labMap.get(solicitud.laboratorioId)
  const materia = materiaMap.get(solicitud.materiaId)
  const laboratorioPropuesto = solicitud.propuestaLaboratorioId ? labMap.get(solicitud.propuestaLaboratorioId) : null

  return <DashboardLayout breadcrumb="Solicitudes / Detalle"><section className="reservas-panel reserva-detail">
    <Link className="reserva-detail__back" to="/reservas">← Volver</Link>
    <header className="reserva-detail__header"><div><p className="reservas-panel__eyebrow">Solicitud de reserva</p><h1>{materia ? `${materia.codigo} — ${materia.nombre}` : 'Detalle de solicitud'}</h1></div><span className={`reserva-card__status reserva-card__status--${solicitud.estado.toLowerCase()}`}>{solicitud.estado.replace(/_/g, ' ')}</span></header>
    <dl className="reserva-detail__data"><div><dt>Laboratorio</dt><dd>{laboratorio ? `${laboratorio.codigo} — ${laboratorio.nombre}` : 'Laboratorio'}</dd></div><div><dt>Fecha</dt><dd>{solicitud.fechaReserva}</dd></div><div><dt>Horario</dt><dd>{solicitud.horaInicio} – {solicitud.horaFin}</dd></div><div><dt>Participantes</dt><dd>{solicitud.numeroParticipantes}</dd></div><div><dt>Motivo</dt><dd>{solicitud.motivo}</dd></div><div><dt>Observación</dt><dd>{solicitud.observacion || 'Sin observación'}</dd></div></dl>

    {solicitud.estado === 'PROPUESTA' && <section className="proposal-box"><h2>Alternativa propuesta</h2><p><strong>Laboratorio:</strong> {laboratorioPropuesto ? `${laboratorioPropuesto.codigo} — ${laboratorioPropuesto.nombre}` : 'Laboratorio propuesto'}</p><p><strong>Fecha y hora:</strong> {solicitud.propuestaFecha} · {solicitud.propuestaHoraInicio} – {solicitud.propuestaHoraFin}</p><p>{solicitud.propuestaObservacion || 'Sin observación adicional.'}</p>{propietario && puedeCrear && <div className="reserva-form__actions"><button disabled={ocupado} onClick={() => void ejecutar(() => responderPropuesta(solicitud.id, true, comentario))}>Aceptar propuesta</button><button disabled={ocupado} onClick={() => void ejecutar(() => responderPropuesta(solicitud.id, false, comentario))}>Rechazar propuesta</button></div>}</section>}

    <label className="action-comment">Comentario<textarea value={comentario} maxLength={2000} onChange={(event) => setComentario(event.target.value)} /></label>
    <div className="request-actions">
      {solicitud.estado === 'PENDIENTE' && puedeAprobar && <button disabled={ocupado} onClick={() => void ejecutar(() => ponerEnRevision(solicitud.id))}>Poner en revisión</button>}
      {solicitud.estado === 'EN_REVISION' && puedeAprobar && <button disabled={ocupado} onClick={() => void ejecutar(() => aprobarSolicitud(solicitud.id, usuario!.perfilId, comentario, approvalKey.current))}>Aprobar</button>}
      {solicitud.estado === 'EN_REVISION' && puedeRechazar && <button disabled={ocupado || !comentario.trim()} onClick={() => void ejecutar(() => rechazarSolicitud(solicitud.id, comentario))}>Rechazar</button>}
      {propietario && hasPermission(usuario, 'SOLICITUD_CANCELAR') && estadosCancelables.has(solicitud.estado) && <button disabled={ocupado || !comentario.trim()} onClick={() => window.confirm('¿Confirma retirar o cancelar esta solicitud?') && void ejecutar(() => cancelarSolicitud(solicitud.id, comentario))}>Cancelar/Retirar</button>}
    </div>

    {solicitud.estado === 'EN_REVISION' && puedeAprobar && <form className="proposal-form" onSubmit={(event) => { event.preventDefault(); void ejecutar(() => proponerAlternativa(solicitud.id, propuesta)) }}><h2>Proponer alternativa</h2><label>Laboratorio<select required value={propuesta.laboratorioId} onChange={(event) => setPropuesta({ ...propuesta, laboratorioId: event.target.value })}>{laboratorios.map((lab) => <option key={lab.id} value={lab.id}>{lab.codigo} — {lab.nombre}</option>)}</select></label><label>Fecha<input required type="date" value={propuesta.fecha} onChange={(event) => setPropuesta({ ...propuesta, fecha: event.target.value })} /></label><label>Hora inicio<input required type="time" value={propuesta.horaInicio} onChange={(event) => setPropuesta({ ...propuesta, horaInicio: event.target.value })} /></label><label>Hora fin<input required type="time" value={propuesta.horaFin} onChange={(event) => setPropuesta({ ...propuesta, horaFin: event.target.value })} /></label><label>Observación<textarea value={propuesta.observacion} onChange={(event) => setPropuesta({ ...propuesta, observacion: event.target.value })} /></label><button disabled={ocupado || propuesta.horaFin <= propuesta.horaInicio}>Enviar propuesta</button></form>}

    <section className="history"><h2>Historial</h2>{historial.length === 0 ? <p>Sin cambios registrados.</p> : <ol>{historial.map((item) => <li key={item.id}><strong>{item.estadoNuevo.replace(/_/g, ' ')}</strong><span>{new Date(item.fechaHora).toLocaleString('es-EC')}</span>{item.comentario && <p>{item.comentario}</p>}</li>)}</ol>}</section>
    {error && <p role="alert" className="reservas-panel__message--error">{error}</p>}
  </section></DashboardLayout>
}
