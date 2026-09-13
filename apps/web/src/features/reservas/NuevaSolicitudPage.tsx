import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { DashboardLayout } from '../../components/DashboardLayout'
import { hasRole, useAuth } from '../../auth'
import { useAcademicPeriod } from '../../academicPeriodContext'
import {
  obtenerDocentePorPerfil, obtenerDocentes, obtenerHorariosDocente,
  obtenerLaboratorios, obtenerMaterias, obtenerPeriodoActual, obtenerPisos,
  type Docente, type HorarioAcademico, type Laboratorio, type Materia, type PeriodoLectivo, type Piso,
} from '../../services/academicoApi'
import { formatPisoLabel, laboratoriosDelPiso } from '../planificacion/planificacionLaboratorioFilter'
import { consultarDisponibilidad, crearSolicitud, type Disponibilidad } from './reservasApi'
import { generarIdempotencyKey } from '../../utils/idempotency'
import './Reservas.css'

const initialForm = { docenteId: '', laboratorioId: '', materiaId: '', periodoLectivoId: '', fechaReserva: '', horaInicio: '', horaFin: '', numeroParticipantes: 1, motivo: '', observacion: '' }

export function obtenerFechaLocalHoy(): string {
  const fecha = new Date()
  const year = fecha.getFullYear()
  const month = String(fecha.getMonth() + 1).padStart(2, '0')
  const day = String(fecha.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function NuevaSolicitudPage() {
  const { usuario } = useAuth()
  const navigate = useNavigate()
  const { periodoSeleccionado, periodoVigente, cargando: cargandoPeriodo } = useAcademicPeriod()
  const [periodoLocal, setPeriodoLocal] = useState<PeriodoLectivo | null>(null)
  const periodo = periodoSeleccionado ?? periodoVigente ?? periodoLocal

  const [form, setForm] = useState(initialForm)
  const [docentes, setDocentes] = useState<Docente[]>([])
  const [horarios, setHorarios] = useState<HorarioAcademico[]>([])
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([])
  const [pisos, setPisos] = useState<Piso[]>([])
  const [pisoFiltroId, setPisoFiltroId] = useState('')
  const [materias, setMaterias] = useState<Materia[]>([])
  const [cargando, setCargando] = useState(true)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [disponibilidad, setDisponibilidad] = useState<Disponibilidad | null>(null)
  const [consultando, setConsultando] = useState(false)
  const idempotencyKey = useRef(generarIdempotencyKey())
  const esDocente = hasRole(usuario, 'DOCENTE')

  const hoy = useMemo(() => obtenerFechaLocalHoy(), [])
  const fechaMinima = useMemo(() => {
    if (!periodo?.fechaInicio) return hoy
    return periodo.fechaInicio < hoy ? hoy : periodo.fechaInicio
  }, [periodo?.fechaInicio, hoy])

  const fechaMaxima = useMemo(() => {
    return periodo?.fechaFin || ''
  }, [periodo?.fechaFin])

  const sinFechasDisponibles = useMemo(() => {
    if (!periodo?.fechaFin) return false
    return hoy > periodo.fechaFin
  }, [periodo?.fechaFin, hoy])

  useEffect(() => {
    if (periodo?.id) {
      setForm((current) => {
        let nuevaFecha = current.fechaReserva
        if (nuevaFecha) {
          const hoyStr = obtenerFechaLocalHoy()
          const fMin = !periodo.fechaInicio || periodo.fechaInicio < hoyStr ? hoyStr : periodo.fechaInicio
          const fMax = periodo.fechaFin || ''
          if (nuevaFecha < fMin || (fMax && nuevaFecha > fMax)) {
            nuevaFecha = ''
          }
        }
        return {
          ...current,
          periodoLectivoId: periodo.id,
          fechaReserva: nuevaFecha,
        }
      })
    }
  }, [periodo?.id, periodo?.fechaInicio, periodo?.fechaFin])

  useEffect(() => {
    if (!usuario?.perfilId) return
    let active = true
    async function cargar() {
      setCargando(true)
      setError(null)
      try {
        const [labs, materiasDisponibles, periodoActual, pisosData] = await Promise.all([
          obtenerLaboratorios(),
          obtenerMaterias(),
          (!periodoSeleccionado && !periodoVigente) ? obtenerPeriodoActual().catch(() => null) : Promise.resolve(null),
          Promise.resolve(typeof obtenerPisos === 'function' ? obtenerPisos() : []).then((p) => Array.isArray(p) ? p : []).catch(() => [] as Piso[]),
        ])
        if (periodoActual) {
          setPeriodoLocal(periodoActual)
        }
        let docentesDisponibles: Docente[]
        let docenteSeleccionado: Docente
        if (esDocente) {
          const porPerfil = await obtenerDocentePorPerfil(usuario!.perfilId).catch(() => null)
          if (porPerfil) {
            docenteSeleccionado = porPerfil
            docentesDisponibles = [porPerfil]
          } else {
            docentesDisponibles = (await obtenerDocentes().catch(() => [] as Docente[])).filter((item) => item.activo)
            if (docentesDisponibles.length === 0) throw new Error('No existen docentes activos disponibles.')
            docenteSeleccionado = docentesDisponibles[0]
          }
        } else {
          docentesDisponibles = (await obtenerDocentes()).filter((item) => item.activo)
          if (docentesDisponibles.length === 0) throw new Error('No existen docentes activos disponibles.')
          docenteSeleccionado = docentesDisponibles[0]
        }
        const horariosDocente = docenteSeleccionado
          ? await obtenerHorariosDocente(docenteSeleccionado.id).catch(() => [] as HorarioAcademico[])
          : []
        if (!active) return
        setLaboratorios(labs.filter((item) => item.activo))
        setPisos(pisosData.filter((item) => item.activo))
        setMaterias(materiasDisponibles.filter((item) => item.activo))
        setDocentes(docentesDisponibles)
        setHorarios(horariosDocente.filter((item) => item.activo))
        const pId = periodo?.id ?? periodoActual?.id
        setForm((current) => ({
          ...current,
          docenteId: docenteSeleccionado.id,
          periodoLectivoId: pId ?? current.periodoLectivoId,
        }))
      } catch (cause) {
        if (active) setError(cause instanceof Error ? cause.message : 'No se pudieron cargar los datos académicos.')
      } finally {
        if (active) setCargando(false)
      }
    }
    void cargar()
    return () => { active = false }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [esDocente, usuario?.perfilId])

  const pisosMap = useMemo(() => new Map(pisos.map((p) => [p.id, p])), [pisos])
  const pisosOrdenados = useMemo(() => [...pisos].sort((a, b) => a.numero - b.numero), [pisos])

  const laboratoriosVisibles = useMemo(() => {
    return laboratoriosDelPiso(laboratorios, pisoFiltroId)
  }, [laboratorios, pisoFiltroId])

  const cambiarPiso = (nuevoPisoId: string) => {
    setPisoFiltroId(nuevoPisoId)
    if (nuevoPisoId && form.laboratorioId) {
      const labActual = laboratorios.find((l) => l.id === form.laboratorioId)
      if (labActual && labActual.pisoId !== nuevoPisoId) {
        cambiar('laboratorioId', '')
      }
    }
  }

  const materiasVisibles = useMemo(() => {
    if (!esDocente) return materias
    const ids = new Set(horarios.filter((h) => h.periodoLectivoId === periodo?.id).map((h) => h.materiaId))
    return ids.size > 0 ? materias.filter((materia) => ids.has(materia.id)) : materias
  }, [esDocente, horarios, materias, periodo])

  const cambiar = (name: string, value: string | number) => {
    setForm((current) => ({ ...current, [name]: value }))
    idempotencyKey.current = generarIdempotencyKey()
    setDisponibilidad(null)
  }

  const cambiarDocente = async (docenteId: string) => {
    cambiar('docenteId', docenteId)
    try { setHorarios((await obtenerHorariosDocente(docenteId)).filter((item) => item.activo)) }
    catch { setHorarios([]) }
  }

  const comprobar = async () => {
    if (sinFechasDisponibles) {
      setError('No existen fechas disponibles para reservas dentro de este período académico.')
      return
    }
    if (form.fechaReserva && (form.fechaReserva < fechaMinima || (fechaMaxima && form.fechaReserva > fechaMaxima))) {
      setError(`La fecha de la reserva debe estar comprendida entre ${periodo?.fechaInicio ?? ''} y ${periodo?.fechaFin ?? ''} para el período académico seleccionado.`)
      return
    }
    setConsultando(true); setError(null)
    try { setDisponibilidad(await consultarDisponibilidad(form.laboratorioId, form.fechaReserva, form.horaInicio, form.horaFin)) }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo consultar la disponibilidad.') }
    finally { setConsultando(false) }
  }

  const enviar = async (event: FormEvent) => {
    event.preventDefault()
    if (enviando || !usuario || !form.periodoLectivoId) return
    if (sinFechasDisponibles) {
      setError('No existen fechas disponibles para reservas dentro de este período académico.')
      return
    }
    if (form.fechaReserva < fechaMinima || (fechaMaxima && form.fechaReserva > fechaMaxima)) {
      setError(`La fecha de la reserva debe estar comprendida entre ${periodo?.fechaInicio ?? ''} y ${periodo?.fechaFin ?? ''} para el período académico seleccionado.`)
      return
    }
    setEnviando(true); setError(null)
    try {
      const solicitud = await crearSolicitud({ ...form, solicitanteId: usuario.perfilId }, idempotencyKey.current)
      navigate(`/solicitudes/${solicitud.id}`, { replace: true })
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No se pudo crear la solicitud.')
    } finally { setEnviando(false) }
  }

  return <DashboardLayout breadcrumb="Reservas / Nueva solicitud"><section className="reservas-panel">
    <h1>Nueva solicitud</h1>
    {cargando && <p role="status">Cargando información académica...</p>}
    {!cargando && !periodo && !cargandoPeriodo && (
      <p role="status" className="reservas-panel__message--warning">
        No existe un período académico activo disponible para la fecha actual.
      </p>
    )}
    {!cargando && periodo && sinFechasDisponibles && (
      <p role="status" className="reservas-panel__message--warning">
        No existen fechas disponibles para reservas dentro de este período académico.
      </p>
    )}
    {!cargando && <form className="reserva-form" onSubmit={enviar}>
      <label>Docente
        <select value={form.docenteId} disabled={esDocente} onChange={(event) => void cambiarDocente(event.target.value)}>
          {docentes.map((docente) => (
            <option key={docente.id} value={docente.id}>
              {docente.nombres && docente.apellidos
                ? `${docente.nombres} ${docente.apellidos} (${docente.codigoDocente || 'DOC'})`
                : (docente.codigoDocente || 'Docente autenticado')}
            </option>
          ))}
        </select>
      </label>
      <label>Materia<select required value={form.materiaId} onChange={(e) => cambiar('materiaId', e.target.value)}><option value="">Seleccione una materia</option>{materiasVisibles.map((m) => <option key={m.id} value={m.id}>{m.codigo} — {m.nombre}</option>)}</select></label>
      <label>Período lectivo<input readOnly value={periodo ? `${periodo.codigo} — ${periodo.nombre}` : 'Sin período lectivo activo'} /></label>
      <label>Piso
        <select value={pisoFiltroId} onChange={(e) => cambiarPiso(e.target.value)}>
          <option value="">Todos los pisos</option>
          {pisosOrdenados.map((piso) => (
            <option key={piso.id} value={piso.id}>
              {formatPisoLabel(piso)}
            </option>
          ))}
        </select>
      </label>
      <label>Laboratorio
        <select required value={form.laboratorioId} onChange={(e) => cambiar('laboratorioId', e.target.value)}>
          <option value="">Seleccione un laboratorio</option>
          {laboratoriosVisibles.map((lab) => {
            const p = pisosMap.get(lab.pisoId)
            const sufijoPiso = p ? ` — ${formatPisoLabel(p)}` : ''
            return (
              <option key={lab.id} value={lab.id}>
                {lab.codigo} — {lab.nombre}{sufijoPiso}
              </option>
            )
          })}
        </select>
      </label>
      <label>Fecha<input required type="date" min={fechaMinima} max={fechaMaxima || undefined} disabled={sinFechasDisponibles} value={form.fechaReserva} onChange={(e) => cambiar('fechaReserva', e.target.value)} /></label>
      <label>Hora inicio<input required type="time" value={form.horaInicio} onChange={(e) => cambiar('horaInicio', e.target.value)} /></label>
      <label>Hora fin<input required type="time" value={form.horaFin} onChange={(e) => cambiar('horaFin', e.target.value)} /></label>
      <label>Participantes<input required min="1" type="number" value={form.numeroParticipantes} onChange={(e) => cambiar('numeroParticipantes', Number(e.target.value))} /></label>
      <label className="reserva-form__wide">Motivo<textarea required maxLength={500} value={form.motivo} onChange={(e) => cambiar('motivo', e.target.value)} /></label>
      <label className="reserva-form__wide">Observación<textarea maxLength={2000} value={form.observacion} onChange={(e) => cambiar('observacion', e.target.value)} /></label>
      <div className="reserva-form__actions"><button type="button" disabled={consultando || sinFechasDisponibles || !form.laboratorioId || !form.fechaReserva || !form.horaInicio || !form.horaFin || form.horaFin <= form.horaInicio} onClick={() => void comprobar()}>{consultando ? 'Consultando...' : 'Comprobar disponibilidad'}</button><button type="submit" disabled={enviando || sinFechasDisponibles || !form.docenteId || !form.materiaId || !form.periodoLectivoId}>{enviando ? 'Enviando...' : 'Crear solicitud'}</button></div>
      {disponibilidad && <p role="status" className={disponibilidad.disponible ? 'availability--ok' : 'availability--conflict'}>{disponibilidad.disponible ? 'Disponible' : `No disponible${disponibilidad.motivo ? `: ${disponibilidad.motivo}` : ''}`}</p>}
    </form>}
    {error && <p role="alert" className="reservas-panel__message--error">{error}</p>}
  </section></DashboardLayout>
}
