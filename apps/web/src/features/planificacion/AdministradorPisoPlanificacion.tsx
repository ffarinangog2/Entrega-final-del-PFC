import { useCallback, useEffect, useMemo, useState } from 'react'
import { DashboardLayout } from '../../components/DashboardLayout'
import {
  obtenerCarreras,
  obtenerLaboratorios,
  obtenerMaterias,
  obtenerPeriodoActual,
  obtenerPisos,
  type Carrera,
  type Laboratorio,
  type Materia,
  type PeriodoLectivo,
  type Piso,
} from '../../services/academicoApi'
import {
  aprobarPlanificacionPiso,
  listarPlanificacionesAgregadas,
  proponerCambioPlanificacionPiso,
  rechazarPlanificacionPiso,
  type PlanificacionAgregada,
  type Planificacion,
  type SolicitudCambio, listarSolicitudesCambio, aprobarSolicitudCambio, rechazarSolicitudCambio,
  type SolicitudRetiro, listarSolicitudesRetiro, aprobarSolicitudRetiro, rechazarSolicitudRetiro,
} from '../../services/operationalApi'
import './AdministradorPisoPlanificacion.css'
import { estadoPaquete } from './adminPisoPlanificacionState'
import { formatPisoLabel } from './planificacionLaboratorioFilter'

const dias = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES']
const horas = Array.from(
  { length: 10 },
  (_, index) => `${String(index + 7).padStart(2, '0')}:30`,
)
type Propuesta = {
  laboratorioId: string
  horaInicio: string
  horaFin: string
  observacion: string
}

export function AdministradorPisoPlanificacion() {
  const [planes, setPlanes] = useState<Planificacion[]>([])
  const [agregados, setAgregados] = useState<PlanificacionAgregada[]>([])
  const [materias, setMaterias] = useState<Materia[]>([])
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([])
  const [carreras, setCarreras] = useState<Carrera[]>([])
  const [periodo, setPeriodo] = useState<PeriodoLectivo | null>(null)
  const [paquete, setPaquete] = useState('')
  const [propuestas, setPropuestas] = useState<Record<string, Propuesta>>({})
  const [rechazo, setRechazo] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [cargando, setCargando] = useState(true)
  const [solicitudes, setSolicitudes] = useState<SolicitudCambio[]>([])
  const [pisos, setPisos] = useState<Piso[]>([])
  const [retiros, setRetiros] = useState<SolicitudRetiro[]>([])
  const [observacionRetiro, setObservacionRetiro] = useState('')

  const cargar = useCallback(async () => {
    setCargando(true)
    setError('')
    try {
      const [
        planesData,
        materiasData,
        laboratoriosData,
        carrerasData,
        periodoData,
        pisosData,
      ] = await Promise.all([
        listarPlanificacionesAgregadas(),
        obtenerMaterias(),
        obtenerLaboratorios(),
        obtenerCarreras(),
        obtenerPeriodoActual(),
        Promise.resolve(typeof obtenerPisos === 'function' ? obtenerPisos() : []).then(p => Array.isArray(p) ? p : []).catch(() => []),
      ])
      setAgregados(planesData)
      setPlanes(planesData.flatMap((item) => item.bloques))
      setMaterias(materiasData)
      setLaboratorios(laboratoriosData)
      setCarreras(carrerasData)
      setPeriodo(periodoData)
      setPisos(pisosData)
      const primera = planesData[0]
      setPaquete((actual) => actual || primera?.id || '')
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No fue posible cargar la planificación de su piso.',
      )
    } finally {
      setCargando(false)
    }
  }, [])
  useEffect(() => void cargar(), [cargar])
  useEffect(() => {
    if (paquete) {
      void Promise.resolve(listarSolicitudesCambio(paquete))
        .then((value) => setSolicitudes(value ?? []))
        .catch(() => setSolicitudes([]))
    }
  }, [paquete, agregados])
  useEffect(() => {
    if (!paquete) return
    void Promise.resolve(listarSolicitudesRetiro(paquete))
      .then((value) => setRetiros(value ?? []))
      .catch(() => setRetiros([]))
  }, [paquete, agregados])

  const paquetes = useMemo(
    () =>
      Array.from(
        new Map(
          agregados.map((item) => [item.id, item]),
        ).entries(),
      ),
    [agregados],
  )
  const visibles = useMemo(
    () =>
      planes.filter(
        (item) => item.planificacionId === paquete && item.estado !== 'CANCELADA',
      ),
    [paquete, planes],
  )
  const planActual = agregados.find((item) => item.id === paquete)
  const pendiente = planActual?.estado === 'EN_REVISION'
  const materia = (id: string) => materias.find((item) => item.id === id)
  const laboratorio = (id: string) =>
    laboratorios.find((item) => item.id === id)
  const carrera = carreras.find(
    (item) => item.id === (planActual?.carreraId ?? visibles[0]?.carreraId),
  )

  const miPisoId = useMemo(() => {
    return (
      planActual?.pisoGestionadoId ??
      agregados.find((item) => item.pisoGestionadoId)?.pisoGestionadoId ??
      laboratorios.find((l) => l.id === visibles[0]?.laboratorioId)?.pisoId ??
      null
    )
  }, [planActual, agregados, laboratorios, visibles])

  const solicitudesAccionables = useMemo(() => {
    return solicitudes.filter((s) => {
      const miRevision = s.revisiones?.find((r) => r.pisoId === miPisoId)
      return s.estado === 'PENDIENTE' && miRevision?.estado === 'PENDIENTE'
    })
  }, [solicitudes, miPisoId])

  const solicitudesInformativas = useMemo(() => {
    return solicitudes
      .filter((s) => s.revisiones?.some((r) => r.pisoId === miPisoId))
      .filter(
        (s) =>
          !(
            s.estado === 'PENDIENTE' &&
            s.revisiones?.find((r) => r.pisoId === miPisoId)?.estado === 'PENDIENTE'
          ),
      )
      .slice(0, 3)
  }, [solicitudes, miPisoId])

  const renderEstadosRevisiones = (s: SolicitudCambio) => {
    if (!s.revisiones || s.revisiones.length === 0) return null
    return (
      <div className="floor-planning__revisions-list" style={{ display: 'flex', flexDirection: 'column', gap: '4px', margin: '6px 0' }}>
        {s.revisiones.map((rev) => {
          const pisoObj = pisos.find((p) => p.id === rev.pisoId)
          const labelPiso = formatPisoLabel(pisoObj)
          const estadoTexto =
            rev.estado === 'APROBADA'
              ? '✓ Aprobado'
              : rev.estado === 'RECHAZADA'
                ? '✕ Rechazado'
                : 'Pendiente'
          const badgeColor =
            rev.estado === 'APROBADA'
              ? '#15803d'
              : rev.estado === 'RECHAZADA'
                ? '#b91c1c'
                : '#b45309'
          return (
            <div key={rev.pisoId} style={{ fontSize: '0.9rem' }}>
              <strong>{labelPiso}:</strong>{' '}
              <span style={{ color: badgeColor, fontWeight: 600 }}>{estadoTexto}</span>
              {rev.observacion ? <span> — {rev.observacion}</span> : null}
            </div>
          )
        })}
      </div>
    )
  }

  const renderDetalleSolicitud = (s: SolicitudCambio) => {
    const labActual = laboratorios.find((l) => l.id === s.laboratorioAnteriorId)
    const labPropuesto = laboratorios.find((l) => l.id === s.laboratorioPropuestoId)
    const pisoActual = pisos.find((p) => p.id === labActual?.pisoId)
    const pisoPropuesto = pisos.find((p) => p.id === labPropuesto?.pisoId)

    const materiaNombre =
      (s.materiaId ? materias.find((m) => m.id === s.materiaId)?.nombre : null) ??
      materias.find((m) => m.id === planes.find((b) => b.id === s.bloqueId)?.materiaId)?.nombre ??
      'Asignatura asignada'

    const diaTexto = s.diaAnterior ? etiquetaDia(s.diaAnterior) : ''
    const horarioTexto = `${diaTexto} · ${s.horaInicioAnterior} - ${s.horaFinAnterior}`

    const textoLabActual = `${labActual?.codigo ?? 'Lab'} · ${formatPisoLabel(pisoActual)}`
    const textoLabPropuesto = `${labPropuesto?.codigo ?? 'Lab'} · ${formatPisoLabel(pisoPropuesto)}`

    if (s.tipo === 'LABORATORIO') {
      return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '3px', margin: '4px 0' }}>
          <span><strong>Asignatura:</strong> {materiaNombre}</span>
          <span><strong>Horario:</strong> {horarioTexto}</span>
          <span><strong>Actual:</strong> {textoLabActual}</span>
          <span><strong>Solicitado:</strong> {textoLabPropuesto}</span>
          <span><strong>Motivo:</strong> {s.motivo}</span>
        </div>
      )
    }

    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '3px', margin: '4px 0' }}>
        <span><strong>Asignatura:</strong> {materiaNombre}</span>
        <span><strong>Horario:</strong> {horarioTexto}</span>
        <span><strong>Motivo:</strong> {s.motivo}</span>
      </div>
    )
  }

  async function ejecutar(
    operacion: () => Promise<unknown>,
    confirmacion: string,
  ) {
    if (ocupado || !window.confirm(confirmacion)) return
    setOcupado(true)
    setError('')
    setMensaje('')
    try {
      await operacion()
      setMensaje('La planificación fue actualizada correctamente.')
      setRechazo('')
      setPropuestas({})
      await cargar()
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No fue posible completar la revisión.',
      )
    } finally {
      setOcupado(false)
    }
  }

  const aprobar = () =>
    ejecutar(
      () => aprobarPlanificacionPiso(paquete),
      `¿Desea aprobar la planificación ${periodo?.codigo ?? ''}?`,
    )
  const rechazar = () => {
    if (!rechazo.trim()) {
      setError('Escriba el motivo del rechazo.')
      return
    }
    void ejecutar(
      () => rechazarPlanificacionPiso(paquete, rechazo.trim()),
      '¿Desea devolver la planificación completa a Coordinación?',
    )
  }
  const enviarPropuestas = () => {
    const marcadas = Object.entries(propuestas).filter(([, value]) =>
      value.observacion.trim(),
    )
    if (marcadas.length === 0) {
      setError('Marque al menos un bloque e indique una observación.')
      return
    }
    void ejecutar(
      () =>
        Promise.all(
          marcadas.map(([id, value]) =>
            proponerCambioPlanificacionPiso(paquete, {
              bloqueId: id,
              laboratorioPropuestoId: value.laboratorioId,
              observacion: value.observacion.trim(),
            }),
          ),
        ),
      '¿Enviar todas las observaciones marcadas a Coordinación?',
    )
  }

  return (
    <DashboardLayout breadcrumb="Planificación recibida">
      <section className="floor-planning">
        <header>
          <div>
            <p>Administración operativa de su piso</p>
            <h1>Planificación recibida</h1>
          </div>
          <button disabled={cargando} onClick={() => void cargar()}>
            Actualizar
          </button>
        </header>
        {cargando && <p role="status">Cargando planificación...</p>}
        {error && (
          <p role="alert" className="floor-planning__error">
            {error}
          </p>
        )}
        {mensaje && (
          <p role="status" className="floor-planning__success">
            {mensaje}
          </p>
        )}
        {!cargando && !error && paquetes.length === 0 && (
          <p>
            No existe una planificación enviada para los laboratorios de su
            piso.
          </p>
        )}
        {paquetes.length > 0 && (
          <>
            <label>
              Planificación
              <select
                value={paquete}
                onChange={(event) => setPaquete(event.target.value)}
              >
                {paquetes.map(([key, item]) => (
                  <option key={key} value={key}>
                    {carreras.find((value) => value.id === item.carreraId)
                      ?.nombre ?? 'Carrera'}{' '}
                    · {periodo?.codigo ?? 'Periodo'}
                  </option>
                ))}
              </select>
            </label>
            <div className="floor-planning__summary">
              <strong>{carrera?.nombre ?? 'Carrera institucional'}</strong>
              <span>Periodo: {periodo?.codigo ?? 'No disponible'}</span>
              <span>Estado: {planActual?.estado ?? estadoPaquete(visibles)}</span>
              <span>{visibles.length} bloques en su piso</span>
            </div>
            {solicitudesAccionables.map((s) => (
              <article
                className="floor-planning__summary"
                key={s.id}
                style={{ display: 'grid', gap: '6px' }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <strong>{s.tipo === 'LABORATORIO' ? 'Cambio de laboratorio' : `Solicitud de cambio · ${s.tipo}`}</strong>
                  <span style={{ backgroundColor: '#fef3c7', color: '#92400e', padding: '2px 8px', borderRadius: '4px', fontWeight: 600, fontSize: '0.85rem' }}>
                    🟡 Cambio pendiente
                  </span>
                </div>
                {renderDetalleSolicitud(s)}
                {renderEstadosRevisiones(s)}
                <span style={{ fontSize: '0.9rem', color: 'var(--color-muted)' }}>El horario original continúa vigente.</span>
                <div style={{ display: 'flex', gap: '8px', marginTop: '6px' }}>
                  <button
                    disabled={ocupado}
                    onClick={() =>
                      void ejecutar(
                        () => aprobarSolicitudCambio(paquete, s.id),
                        '¿Aprobar y revalidar este cambio?',
                      )
                    }
                  >
                    Aprobar cambio
                  </button>
                  <button
                    disabled={ocupado}
                    onClick={() => {
                      const motivo = window.prompt('Motivo del rechazo')
                      if (motivo?.trim())
                        void ejecutar(
                          () =>
                            rechazarSolicitudCambio(
                              paquete,
                              s.id,
                              motivo.trim(),
                            ),
                          '¿Rechazar esta solicitud?',
                        )
                    }}
                  >
                    Rechazar cambio
                  </button>
                </div>
              </article>
            ))}
            {solicitudesInformativas.map((s) => {
              const esAprobada = s.estado === 'APROBADA'
              const esRechazada = s.estado === 'RECHAZADA'
              const todasPendientes = s.revisiones?.every((r) => r.estado === 'PENDIENTE')
              const mensajeExplicativo = esAprobada
                ? 'El cambio fue aprobado por todos los pisos involucrados.'
                : esRechazada
                  ? 'El cambio solicitado no fue aplicado.'
                  : todasPendientes
                    ? 'Esperando aprobación de los pisos involucrados.'
                    : 'El cambio se aplicará cuando todos los pisos requeridos lo aprueben.'
              return (
                <article
                  className="floor-planning__summary"
                  key={s.id}
                  style={{
                    display: 'grid',
                    gap: '6px',
                    borderLeft: esAprobada ? '4px solid #16a34a' : esRechazada ? '4px solid #dc2626' : '4px solid #d97706',
                    backgroundColor: esAprobada ? '#f0fdf4' : esRechazada ? '#fef2f2' : '#fffbeb',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <strong>{s.tipo === 'LABORATORIO' ? 'Cambio de laboratorio' : `Solicitud de cambio · ${s.tipo}`}</strong>
                    <span
                      style={{
                        padding: '2px 8px',
                        borderRadius: '4px',
                        fontWeight: 600,
                        fontSize: '0.85rem',
                        backgroundColor: esAprobada ? '#dcfce7' : esRechazada ? '#fee2e2' : '#fef3c7',
                        color: esAprobada ? '#15803d' : esRechazada ? '#991b1b' : '#92400e',
                      }}
                    >
                      {esAprobada ? '🟢 Cambio aprobado' : esRechazada ? '🔴 Cambio rechazado' : '🟡 Cambio pendiente'}
                    </span>
                  </div>
                  {renderDetalleSolicitud(s)}
                  {renderEstadosRevisiones(s)}
                  <p style={{ margin: '4px 0', fontStyle: 'italic', fontSize: '0.9rem' }}>{mensajeExplicativo}</p>
                  {esRechazada && s.resolucion ? (
                    <div style={{ color: '#991b1b', fontSize: '0.9rem' }}>
                      <strong>Motivo del rechazo:</strong> {s.resolucion}
                    </div>
                  ) : null}
                </article>
              )
            })}
            {retiros.filter((item) => item.estado === 'PENDIENTE').map((item) => (
              <article className="floor-planning__summary" key={item.id}>
                <strong>Solicitud de retiro para edici&oacute;n</strong>
                <span>{item.motivo}</span>
                <time>{new Date(item.creadaEn).toLocaleString()}</time>
                <label>Observaci&oacute;n opcional
                  <textarea value={observacionRetiro} onChange={(event) => setObservacionRetiro(event.target.value)} />
                </label>
                <button disabled={ocupado} onClick={() => void ejecutar(
                  () => aprobarSolicitudRetiro(paquete, item.id, observacionRetiro),
                  'Autorizar el retiro para edicion?',
                )}>Aprobar retiro</button>
                <button className="danger" disabled={ocupado} onClick={() => void ejecutar(
                  () => rechazarSolicitudRetiro(paquete, item.id, observacionRetiro),
                  'Rechazar el retiro para edicion?',
                )}>Rechazar retiro</button>
              </article>
            ))}
            <div className="floor-planning__grid-wrap">
              <table className="floor-planning__grid">
                <thead>
                  <tr>
                    <th>Hora</th>
                    {dias.map((dia) => (
                      <th key={dia}>{etiquetaDia(dia)}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {horas.map((hora) => (
                    <tr key={hora}>
                      <th>{hora}&ndash;{`${String(Number(hora.slice(0, 2)) + 1).padStart(2, '0')}:30`}</th>
                      {dias.map((dia) => {
                        const fin = `${String(Number(hora.slice(0, 2)) + 1).padStart(2, '0')}:30`
                        const bloques = visibles.filter(
                          (item) =>
                            item.diaSemana === dia &&
                            item.horaInicio >= hora &&
                            item.horaInicio < fin,
                        )
                        return (
                          <td key={dia}>
                            {bloques.map((item) => (
                              <article key={item.id}>
                                <strong>
                                  {materia(item.materiaId)?.nombre ??
                                    'Materia asignada'}
                                </strong>
                                <span>
                                  {item.horaInicio}&ndash;{item.horaFin}
                                </span>
                                <span>
                                  {laboratorio(item.laboratorioId)?.codigo ??
                                    'Laboratorio'}
                                </span>
                                <span>Docente asignado</span>
                                {item.observacion && (
                                  <em>{item.observacion}</em>
                                )}
                                {pendiente && (
                                  <button
                                    type="button"
                                    onClick={() =>
                                      setPropuestas((actual) =>
                                        actual[item.id]
                                          ? Object.fromEntries(
                                              Object.entries(actual).filter(
                                                ([id]) => id !== item.id,
                                              ),
                                            )
                                          : {
                                              ...actual,
                                              [item.id]: {
                                                laboratorioId:
                                                  item.laboratorioId,
                                                horaInicio:
                                                  item.horaInicio.slice(0, 5),
                                                horaFin: item.horaFin.slice(
                                                  0,
                                                  5,
                                                ),
                                                observacion: '',
                                              },
                                            },
                                      )
                                    }
                                  >
                                    {propuestas[item.id]
                                      ? 'Quitar observación'
                                      : 'Marcar cambio'}
                                  </button>
                                )}
                              </article>
                            ))}
                          </td>
                        )
                      })}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {Object.entries(propuestas).map(([id, value]) => {
              const item = visibles.find((plan) => plan.id === id)
              if (!item) return null
              return (
                <fieldset key={id} className="floor-planning__proposal">
                  <legend>
                    {etiquetaDia(item.diaSemana)} {item.horaInicio}&ndash;
                    {item.horaFin}
                  </legend>
                  <label>
                    Laboratorio propuesto
                    <select
                      value={value.laboratorioId}
                      onChange={(event) =>
                        setPropuestas({
                          ...propuestas,
                          [id]: { ...value, laboratorioId: event.target.value },
                        })
                      }
                    >
                      {laboratorios.map((lab) => (
                        <option key={lab.id} value={lab.id}>
                          {lab.codigo} &mdash; {lab.nombre}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Hora inicio
                    <input
                      type="time"
                      value={value.horaInicio}
                      onChange={(event) =>
                        setPropuestas({
                          ...propuestas,
                          [id]: { ...value, horaInicio: event.target.value },
                        })
                      }
                    />
                  </label>
                  <label>
                    Hora fin
                    <input
                      type="time"
                      value={value.horaFin}
                      onChange={(event) =>
                        setPropuestas({
                          ...propuestas,
                          [id]: { ...value, horaFin: event.target.value },
                        })
                      }
                    />
                  </label>
                  <label>
                    Observación
                    <textarea
                      required
                      value={value.observacion}
                      onChange={(event) =>
                        setPropuestas({
                          ...propuestas,
                          [id]: { ...value, observacion: event.target.value },
                        })
                      }
                    />
                  </label>
                </fieldset>
              )
            })}
            {pendiente && (
              <div className="floor-planning__actions">
                <button disabled={ocupado} onClick={() => void aprobar()}>
                  Aprobar planificación
                </button>
                <button
                  disabled={ocupado || Object.keys(propuestas).length === 0}
                  onClick={enviarPropuestas}
                >
                  Enviar observaciones/propuestas
                </button>
                <label>
                  Motivo del rechazo
                  <textarea
                    value={rechazo}
                    onChange={(event) => setRechazo(event.target.value)}
                  />
                </label>
                <button
                  className="danger"
                  disabled={ocupado || !rechazo.trim()}
                  onClick={rechazar}
                >
                  Rechazar planificación
                </button>
              </div>
            )}
          </>
        )}
      </section>
    </DashboardLayout>
  )
}

const etiquetaDia = (dia: string) =>
  dia === 'MIERCOLES'
    ? 'Miércoles'
    : dia.toLowerCase().replace(/^./, (value) => value.toUpperCase())
