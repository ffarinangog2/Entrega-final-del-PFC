import { useCallback, useEffect, useMemo, useState } from 'react'
import { DashboardLayout } from '../../components/DashboardLayout'
import {
  obtenerCarreras,
  obtenerDocentesPlanificacion,
  obtenerLaboratorios,
  obtenerMaterias,
  obtenerPeriodoActual,
  obtenerPisos,
  type Carrera,
  type Docente,
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
  const [pisos, setPisos] = useState<Piso[]>([])
  const [docentes, setDocentes] = useState<Docente[]>([])
  const [solicitudes, setSolicitudes] = useState<SolicitudCambio[]>([])
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
        docentesData,
      ] = await Promise.all([
        listarPlanificacionesAgregadas(),
        obtenerMaterias(),
        obtenerLaboratorios(),
        obtenerCarreras(),
        obtenerPeriodoActual(),
        obtenerPisos(),
        obtenerDocentesPlanificacion(),
      ])
      const planesConBloques = planesData.filter((item) =>
        item.bloques.some((bloque) => bloque.estado !== 'CANCELADA'),
      )
      setAgregados(planesConBloques)
      setPlanes(planesConBloques.flatMap((item) => item.bloques))
      setMaterias(materiasData)
      setLaboratorios(laboratoriosData)
      setCarreras(carrerasData)
      setPeriodo(periodoData)
      setPisos(pisosData)
      setDocentes(docentesData)
      const primera = planesConBloques[0]
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
  useEffect(()=>{if(paquete)void Promise.resolve(listarSolicitudesCambio(paquete)).then(value=>setSolicitudes(value??[])).catch(()=>setSolicitudes([]))},[paquete,agregados])
  useEffect(() => {
    if (!paquete) return
    void Promise.resolve(listarSolicitudesRetiro(paquete))
      .then((value) => setRetiros(value ?? [])).catch(() => setRetiros([]))
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
  const materia = (id: string) => materias.find((item) => item.id === id)
  const laboratorio = (id: string) =>
    laboratorios.find((item) => item.id === id)
  const docente = (id?: string | null) =>
    docentes.find((item) => item.id === id)
  const planActual = agregados.find((item) => item.id === paquete)
  const pisoActual = laboratorio(visibles[0]?.laboratorioId ?? '')?.pisoId

  const nombrePiso = (pisoId?: string) => {
    if (!pisoId) return 'Sin piso'
    const p = pisos.find((item) => item.id === pisoId)
    return p?.descripcion?.trim() ? p.descripcion : (p ? `Piso ${p.numero}` : 'Sin piso')
  }

  const pisoDeLaboratorio = (labId?: string | null) => {
    if (!labId) return 'Sin piso'
    const lab = laboratorio(labId)
    if (!lab) return 'Sin piso'
    return nombrePiso(lab.pisoId)
  }
  const miRevision = planActual?.revisiones?.find(
    (item) => item.vigente === true && item.pisoId === pisoActual,
  )
  const pendiente = miRevision ? miRevision.estado === 'PENDIENTE' : planActual?.estado === 'EN_REVISION'
  const carrera = carreras.find((item) => item.id === visibles[0]?.carreraId)

  async function ejecutarSolicitudCambio(
    operacion: () => Promise<unknown>,
    confirmacion: string,
  ) {
    if (ocupado || !window.confirm(confirmacion)) return
    setOcupado(true)
    setError('')
    setMensaje('')
    try {
      await operacion()
      setMensaje('La solicitud de cambio fue procesada correctamente.')
      if (paquete) {
        const actualizadas = await listarSolicitudesCambio(paquete)
        setSolicitudes(actualizadas ?? [])
      }
      await cargar()
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No fue posible procesar la solicitud de cambio.',
      )
    } finally {
      setOcupado(false)
    }
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
              {miRevision?.estado === 'APROBADA' && <span className="status">Aprobada por este piso</span>}
              <span>{visibles.length} bloques en su piso</span>
            </div>
            {solicitudes.map((s) => {
              const miRevision = s.revisiones?.find((r) => r.pisoId === pisoActual)
              const puedeDecidir = s.estado === 'PENDIENTE' && (!miRevision || miRevision.estado === 'PENDIENTE')
              const b = planes.find((p) => p.id === s.bloqueId)
              const mat = materia(b?.materiaId ?? '')
              const diaTxt = etiquetaDia(s.diaAnterior)
              const pisoOrig = pisoDeLaboratorio(s.laboratorioAnteriorId)
              const pisoDest = pisoDeLaboratorio(s.laboratorioPropuestoId)
              const labOrig = laboratorio(s.laboratorioAnteriorId)
              const labDest = laboratorio(s.laboratorioPropuestoId)
              const docOrig = docente(s.docenteAnteriorId)
              const docDest = docente(s.docentePropuestoId)

              return (
                <article className="floor-planning__summary" key={s.id}>
                  <strong>Solicitud de cambio · {s.tipo}</strong>
                  <span>Solicitante: Coordinación académica</span>
                  <span>
                    Bloque afectado: {mat?.nombre ?? 'Materia'} ({diaTxt} {s.horaInicioAnterior}–{s.horaFinAnterior})
                  </span>
                  {s.tipo === 'LABORATORIO' && (
                    <>
                      <span>
                        Laboratorio: {labOrig?.codigo ?? 'Actual'} ({pisoOrig}) → {labDest?.codigo ?? 'Propuesto'} ({pisoDest})
                      </span>
                      <span>
                        Piso: {pisoOrig} → {pisoDest}
                      </span>
                    </>
                  )}
                  {s.tipo === 'HORARIO' && (
                    <span>
                      Horario propuesto: {etiquetaDia(s.diaPropuesto)} {s.horaInicioPropuesta}–{s.horaFinPropuesta}
                    </span>
                  )}
                  {s.tipo === 'DOCENTE' && (
                    <span>
                      Docente propuesto: {docOrig?.codigoDocente ?? 'Docente actual'} → {docDest?.codigoDocente ?? 'Docente propuesto'}
                    </span>
                  )}
                  {s.tipo === 'CANCELACION' && (
                    <span>Propuesta: Cancelación excepcional del bloque</span>
                  )}
                  <span>Motivo: {s.motivo}</span>
                  {s.estado === 'PENDIENTE' && (
                    <small>El horario original continúa vigente mientras la solicitud esté pendiente.</small>
                  )}
                  {miRevision?.estado === 'APROBADA' && (
                    <span className="status">Cambio aprobado por este piso</span>
                  )}
                  {miRevision?.estado === 'RECHAZADA' && (
                    <span className="status danger">Cambio rechazado por este piso</span>
                  )}
                  {s.estado === 'APROBADA' && (
                    <span className="status">Solicitud de cambio aprobada</span>
                  )}
                  {s.estado === 'RECHAZADA' && !miRevision && (
                    <span className="status danger">Solicitud de cambio rechazada</span>
                  )}
                  {puedeDecidir && (
                    <div>
                      <button
                        disabled={ocupado}
                        onClick={() =>
                          void ejecutarSolicitudCambio(
                            () => aprobarSolicitudCambio(paquete, s.id),
                            '¿Aprobar y revalidar este cambio?',
                          )
                        }
                      >
                        Aprobar cambio
                      </button>
                      <button
                        className="danger"
                        disabled={ocupado}
                        onClick={() => {
                          const mot = window.prompt('Motivo del rechazo')
                          if (mot?.trim()) {
                            void ejecutarSolicitudCambio(
                              () => rechazarSolicitudCambio(paquete, s.id, mot.trim()),
                              '¿Rechazar esta solicitud?',
                            )
                          }
                        }}
                      >
                        Rechazar cambio
                      </button>
                    </div>
                  )}
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
                                  {item.horaInicio}–{item.horaFin}
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
                    {etiquetaDia(item.diaSemana)} {item.horaInicio}–
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
                          {lab.codigo} — {lab.nombre}
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
