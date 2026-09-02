import { useCallback, useEffect, useMemo, useState } from 'react'
import { DashboardLayout } from '../../components/DashboardLayout'
import {
  obtenerCarreras,
  obtenerLaboratorios,
  obtenerMaterias,
  obtenerPeriodoActual,
  type Carrera,
  type Laboratorio,
  type Materia,
  type PeriodoLectivo,
} from '../../services/academicoApi'
import {
  aprobarPlanificacionPiso,
  listarPlanificacionesAgregadas,
  proponerCambioPlanificacionPiso,
  rechazarPlanificacionPiso,
  type PlanificacionAgregada,
  type Planificacion,
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
      ] = await Promise.all([
        listarPlanificacionesAgregadas(),
        obtenerMaterias(),
        obtenerLaboratorios(),
        obtenerCarreras(),
        obtenerPeriodoActual(),
      ])
      setAgregados(planesData)
      setPlanes(planesData.flatMap((item) => item.bloques))
      setMaterias(materiasData)
      setLaboratorios(laboratoriosData)
      setCarreras(carrerasData)
      setPeriodo(periodoData)
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
  const carrera = carreras.find((item) => item.id === visibles[0]?.carreraId)

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
        {!cargando && paquetes.length === 0 && (
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
                      <th>{hora}</th>
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
