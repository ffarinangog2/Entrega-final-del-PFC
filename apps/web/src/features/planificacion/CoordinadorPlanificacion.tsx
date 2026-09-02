import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type FormEvent,
} from 'react'
import { DashboardLayout } from '../../components/DashboardLayout'
import * as academico from '../../services/academicoApi'
import * as api from '../../services/operationalApi'
import './CoordinadorPlanificacion.css'

const dias = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES'] as const
const horas = Array.from(
  { length: 10 },
  (_, index) => `${String(index + 7).padStart(2, '0')}:30`,
)
const inicial: api.GuardarPlanificacion = {
  planificacionId: '',
  nivel: 1,
  periodoId: '',
  carreraId: '',
  materiaId: '',
  docenteId: null,
  laboratorioId: '',
  diaSemana: 'LUNES',
  horaInicio: '07:30',
  horaFin: '08:30',
  observacion: '',
}
const etiquetas: Record<api.EstadoPlanificacion, string> = {
  BORRADOR: 'Borrador',
  ENVIADA: 'En revisión',
  PROPUESTA_CAMBIO: 'Devuelta con propuesta',
  CONFIRMADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
  CANCELADA: 'Retirada',
}

export function CoordinadorPlanificacion() {
  const [items, setItems] = useState<api.Planificacion[]>([])
  const [plan, setPlan] = useState<api.PlanificacionAgregada | null>(null)
  const [planesAgregados, setPlanesAgregados] = useState<api.PlanificacionAgregada[]>([])
  const [nivel, setNivel] = useState(1)
  const [periodos, setPeriodos] = useState<academico.PeriodoLectivo[]>([])
  const [periodoId, setPeriodoId] = useState('')
  const [form, setForm] = useState(inicial)
  const [editandoId, setEditandoId] = useState<string | null>(null)
  const [catalogos, setCatalogos] = useState<{
    periodo?: academico.PeriodoLectivo
    carrera?: academico.Carrera
    materias: academico.Materia[]
    docentes: academico.Docente[]
    laboratorios: academico.Laboratorio[]
  }>({ materias: [], docentes: [], laboratorios: [] })
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [editorAbierto, setEditorAbierto] = useState(false)
  const [confirmando, setConfirmando] = useState(false)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [iniciado, setIniciado] = useState(false)

  const cargar = useCallback(async () => {
    setCargando(true)
    setError('')
    try {
      const [planes, ciclos, materias, docentes, laboratorios, carreras] =
        await Promise.all([
          api.listarPlanificacionesAgregadas(),
          academico.obtenerPeriodos(),
          academico.obtenerMaterias(),
          academico.obtenerDocentesPlanificacion(),
          academico.obtenerLaboratorios(),
          academico.obtenerCarreras(),
        ])
      const carreraIds = [
        ...new Set(materias.map((materia) => materia.carreraId)),
      ]
      if (carreraIds.length !== 1)
        throw new Error(
          'No se pudo determinar una única carrera institucional activa.',
        )
      const carrera = carreras.find((item) => item.id === carreraIds[0])
      if (!carrera)
        throw new Error(
          'No se encontró la carrera institucional del coordinador.',
        )
      const ciclosPpa = ciclos.filter(
        (item) => item.ppaCodigo === 'REGULAR-2026-2027-PPA',
      )
      const cicloId = ciclosPpa[0]?.id || ciclos[0]?.id || ''
      const periodo = ciclos.find((item) => item.id === cicloId)
      if (!periodo) throw new Error('No existe un ciclo académico disponible.')
      const planActual =
        planes.find((item) => item.periodoId === cicloId) ?? null
      setPeriodos(ciclosPpa.length > 0 ? ciclosPpa : ciclos)
      setPeriodoId(cicloId)
      setPlan(planActual)
      setPlanesAgregados(planes)
      setItems(planActual?.bloques ?? [])
      setIniciado(planActual !== null)
      setCatalogos({
        periodo,
        carrera,
        materias: materias.filter((item) => item.activo),
        docentes: docentes.filter((item) => item.activo),
        laboratorios: laboratorios.filter((item) => item.activo),
      })
      setForm((actual) => ({
        ...actual,
        periodoId: periodo.id,
        carreraId: carrera.id,
        planificacionId: planActual?.id ?? '',
        nivel: 1,
      }))
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No se pudo cargar la planificación semanal.',
      )
    } finally {
      setCargando(false)
    }
  }, [])
  useEffect(() => {
    void cargar()
  }, [cargar])

  const visibles = useMemo(
    () => items.filter((item) => item.estado !== 'CANCELADA'),
    [items],
  )
  const visiblesNivel = useMemo(
    () => visibles.filter((item) => (item.nivel ?? 1) === nivel),
    [nivel, visibles],
  )
  const materiasNivel = useMemo(
    () =>
      catalogos.materias.filter(
        (item) => item.nivel == null || item.nivel === nivel,
      ),
    [catalogos.materias, nivel],
  )

  function seleccionarPeriodo(id: string) {
    setPeriodoId(id)
    const periodo = periodos.find((item) => item.id === id)
    const actual = planesAgregados.find((item) => item.periodoId === id) ?? null
    setCatalogos((value) => ({ ...value, periodo }))
    setPlan(actual)
    setItems(actual?.bloques ?? [])
    setIniciado(actual !== null)
  }
  const editables = visibles.filter((item) =>
    ['BORRADOR', 'PROPUESTA_CAMBIO'].includes(item.estado),
  )
  const estadoGeneral = visibles.some(
    (item) => item.estado === 'PROPUESTA_CAMBIO',
  )
    ? 'PROPUESTA_CAMBIO'
    : visibles.some((item) => item.estado === 'BORRADOR')
      ? 'BORRADOR'
      : visibles.length > 0 &&
          visibles.every((item) => item.estado === 'CONFIRMADA')
        ? 'CONFIRMADA'
        : visibles.some((item) => item.estado === 'ENVIADA')
          ? 'ENVIADA'
          : visibles.length > 0
            ? 'BORRADOR'
            : null
  const soloLectura =
    plan?.estado === 'EN_REVISION' ||
    plan?.estado === 'APROBADA' ||
    plan?.estado === 'FINALIZADA'
  const materia = (id: string) =>
    catalogos.materias.find((item) => item.id === id)
  const docente = (id: string | null) =>
    catalogos.docentes.find((item) => item.id === id)
  const laboratorio = (id: string) =>
    catalogos.laboratorios.find((item) => item.id === id)

  function abrirNuevo(diaSemana: string, horaInicio: string) {
    const fin = `${String(Number(horaInicio.slice(0, 2)) + 1).padStart(2, '0')}:30`
    setEditandoId(null)
    setError('')
    setForm({
      ...inicial,
      planificacionId: plan?.id ?? '',
      nivel,
      periodoId: catalogos.periodo?.id ?? '',
      carreraId: catalogos.carrera?.id ?? '',
      diaSemana,
      horaInicio,
      horaFin: fin,
    })
    setEditorAbierto(true)
  }
  function editar(item: api.Planificacion) {
    setEditandoId(item.id)
    setError('')
    setForm({
      planificacionId: item.planificacionId ?? plan?.id ?? '',
      nivel: item.nivel ?? nivel,
      periodoId: item.periodoId,
      carreraId: item.carreraId,
      materiaId: item.materiaId,
      docenteId: item.docenteId,
      laboratorioId: item.laboratorioId,
      diaSemana: item.diaSemana,
      horaInicio: item.horaInicio,
      horaFin: item.horaFin,
      observacion: item.observacion ?? '',
    })
    setEditorAbierto(true)
  }
  function conflictoLocal() {
    const solapados = visibles.filter(
      (item) =>
        item.id !== editandoId &&
        item.diaSemana === form.diaSemana &&
        form.horaInicio < item.horaFin &&
        form.horaFin > item.horaInicio,
    )
    const laboratorioOcupado = solapados.find(
      (item) => item.laboratorioId === form.laboratorioId,
    )
    if (laboratorioOcupado)
      return `${laboratorio(form.laboratorioId)?.codigo ?? 'El laboratorio'} no está disponible el ${form.diaSemana.toLowerCase()} de ${form.horaInicio} a ${form.horaFin}.`
    const docenteOcupado = solapados.find(
      (item) => item.docenteId && item.docenteId === form.docenteId,
    )
    if (docenteOcupado)
      return 'El docente seleccionado ya tiene otra asignación en ese horario.'
    return ''
  }
  async function guardar(event: FormEvent) {
    event.preventDefault()
    setError('')
    setMensaje('')
    if (
      form.horaInicio < '07:30' ||
      form.horaFin > '17:30' ||
      form.horaInicio >= form.horaFin
    ) {
      setError('Seleccione un horario válido entre 07:30 y 17:30.')
      return
    }
    const conflicto = conflictoLocal()
    if (conflicto) {
      setError(conflicto)
      return
    }
    setGuardando(true)
    try {
      if (editandoId) await api.editarPlanificacion(editandoId, form)
      else await api.crearPlanificacion(form)
      setMensaje(
        editandoId
          ? 'Asignación actualizada.'
          : 'Asignación añadida al borrador semanal.',
      )
      setEditorAbierto(false)
      setEditandoId(null)
      await cargar()
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No fue posible guardar la asignación.',
      )
    } finally {
      setGuardando(false)
    }
  }
  async function eliminar(item: api.Planificacion) {
    if (!confirm('¿Eliminar esta asignación del borrador semanal?')) return
    try {
      await api.accionPlanificacion(item.id, 'cancelar')
      setMensaje('Asignación retirada del borrador.')
      await cargar()
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No se pudo retirar la asignación.',
      )
    }
  }
  async function enviarCompleta() {
    if (enviando || !plan || editables.length === 0) return
    setEnviando(true)
    setError('')
    try {
      await api.enviarPlanificacionCompleta(plan.id)
      setConfirmando(false)
      setMensaje('Planificación enviada correctamente para revisión.')
      await cargar()
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No fue posible enviar la planificación completa. Revise su estado antes de reintentar.',
      )
    } finally {
      setEnviando(false)
    }
  }

  async function iniciarPlanificacion() {
    if (!periodoId || guardando) return
    setGuardando(true)
    setError('')
    try {
      const creada = await api.iniciarPlanificacion(periodoId)
      setPlan(creada)
      setItems(creada.bloques)
      setIniciado(true)
      setMensaje('Planificación iniciada en borrador.')
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No fue posible iniciar la planificación.',
      )
    } finally {
      setGuardando(false)
    }
  }

  const resumen = {
    materias: new Set(visibles.map((item) => item.materiaId)).size,
    docentes: new Set(visibles.map((item) => item.docenteId).filter(Boolean))
      .size,
    laboratorios: new Set(visibles.map((item) => item.laboratorioId)).size,
  }
  return (
    <DashboardLayout breadcrumb="Planificación semanal">
      <main className="weekly-planning">
        <header className="weekly-planning__header">
          <div>
            <p>Coordinación académica</p>
            <h1>Planificación semanal</h1>
            <span>
              {catalogos.carrera?.nombre ?? 'Mi carrera'} ·{' '}
              {catalogos.periodo?.ppaNombre ?? catalogos.periodo?.nombre ?? 'Periodo activo'}
            </span>
          </div>
          <div>
            <span
              className={`weekly-planning__status status--${(estadoGeneral ?? 'BORRADOR').toLowerCase()}`}
            >
              {estadoGeneral ? etiquetas[estadoGeneral] : 'Sin iniciar'}
            </span>
            <button onClick={() => void cargar()} disabled={cargando}>
              Actualizar
            </button>
          </div>
        </header>
        {error && (
          <p role="alert" className="operations__error">
            {error}
          </p>
        )}
        {mensaje && (
          <p role="status" className="operations__success">
            {mensaje}
          </p>
        )}
        {cargando ? (
          <p role="status">Cargando planificación...</p>
        ) : (
          <>
            <section
              className="weekly-planning__selectors"
              aria-label="Periodo, ciclo y nivel"
            >
              <label>
                Periodo
                <input
                  readOnly
                  value={catalogos.periodo?.ppaNombre ?? 'REGULAR - 2026-2027 PPA'}
                />
              </label>
              <label>
                Ciclo académico
                <select
                  value={periodoId}
                  onChange={(event) => seleccionarPeriodo(event.target.value)}
                >
                  {periodos.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.cicloAcademico === 1
                        ? 'Mayo–Septiembre'
                        : item.cicloAcademico === 2
                          ? 'Noviembre–Abril'
                          : item.nombre}
                    </option>
                  ))}
                </select>
              </label>
              <div role="group" aria-label="Nivel académico">
                {Array.from({ length: 10 }, (_, index) => index + 1).map(
                  (value) => (
                    <button
                      key={value}
                      aria-pressed={nivel === value}
                      onClick={() => setNivel(value)}
                    >
                      {value}°
                    </button>
                  ),
                )}
              </div>
            </section>
            {estadoGeneral === 'PROPUESTA_CAMBIO' && (
              <aside className="weekly-planning__notice">
                <strong>Planificación devuelta con observaciones.</strong>
                <p>
                  Revise los bloques señalados, sus alternativas y vuelva a
                  enviar la planificación completa.
                </p>
              </aside>
            )}
            <section
              className="weekly-planning__summary"
              aria-label="Resumen de planificación"
            >
              <div>
                <strong>{materiasNivel.length}</strong>
                <span>Materias disponibles</span>
              </div>
              <div>
                <strong>{catalogos.docentes.length}</strong>
                <span>Docentes disponibles</span>
              </div>
              <div>
                <strong>{catalogos.laboratorios.length}</strong>
                <span>Laboratorios disponibles</span>
              </div>
              <div>
                <strong>{visibles.length}</strong>
                <span>Bloques</span>
              </div>
            </section>
            {visibles.length === 0 && (
              <div className="weekly-planning__empty">
                <p>La planificación todavía no tiene bloques.</p>
                {!iniciado && (
                  <button
                    onClick={() => void iniciarPlanificacion()}
                    disabled={guardando}
                  >
                    Iniciar planificación
                  </button>
                )}
                {iniciado && (
                  <p>Seleccione un bloque de la cuadrícula para comenzar.</p>
                )}
              </div>
            )}
            <div className="weekly-planning__grid-wrap">
              <table className="weekly-planning__grid">
                <thead>
                  <tr>
                    <th>Hora</th>
                    {dias.map((dia) => (
                      <th key={dia}>
                        {dia === 'MIERCOLES'
                          ? 'Miércoles'
                          : dia.charAt(0) + dia.slice(1).toLowerCase()}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {horas.map((hora) => (
                    <tr key={hora}>
                      <th>
                        {hora}–
                        {`${String(Number(hora.slice(0, 2)) + 1).padStart(2, '0')}:30`}
                      </th>
                      {dias.map((dia) => {
                        const siguienteHora = `${String(Number(hora.slice(0, 2)) + 1).padStart(2, '0')}:30`
                        const bloques = visiblesNivel.filter(
                          (item) =>
                            item.diaSemana === dia &&
                            item.horaInicio >= hora &&
                            item.horaInicio < siguienteHora,
                        )
                        return (
                          <td key={dia}>
                            {bloques.map((item) => (
                              <article
                                key={item.id}
                                className={`weekly-block weekly-block--${item.estado.toLowerCase()}`}
                              >
                                <strong>
                                  {materia(item.materiaId)?.nombre ?? 'Materia'}
                                </strong>
                                <span>
                                  {docente(item.docenteId)?.codigoDocente ??
                                    'Docente por asignar'}
                                </span>
                                <span>
                                  {laboratorio(item.laboratorioId)?.codigo ??
                                    'Laboratorio'}
                                </span>
                                <time>
                                  {item.horaInicio}–{item.horaFin}
                                </time>
                                {item.observacion && (
                                  <small>{item.observacion}</small>
                                )}
                                {!soloLectura &&
                                  ['BORRADOR', 'PROPUESTA_CAMBIO'].includes(
                                    item.estado,
                                  ) && (
                                    <div>
                                      <button onClick={() => editar(item)}>
                                        Editar
                                      </button>
                                      <button
                                        onClick={() => void eliminar(item)}
                                      >
                                        Eliminar
                                      </button>
                                    </div>
                                  )}
                              </article>
                            ))}
                            {iniciado &&
                              !soloLectura &&
                              bloques.length === 0 && (
                                <button
                                  className="weekly-planning__add"
                                  aria-label={`Agregar ${dia} ${hora}`}
                                  onClick={() => abrirNuevo(dia, hora)}
                                >
                                  +
                                </button>
                              )}
                          </td>
                        )
                      })}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {!soloLectura && editables.length > 0 && (
              <div className="weekly-planning__send">
                <button onClick={() => setConfirmando(true)}>
                  Enviar planificación completa
                </button>
              </div>
            )}
          </>
        )}
        {editorAbierto && (
          <div
            className="planning-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="assignment-title"
          >
            <form onSubmit={guardar}>
              <h2 id="assignment-title">
                {editandoId ? 'Editar asignación' : 'Nueva asignación'}
              </h2>
              <label>
                Materia
                <select
                  required
                  value={form.materiaId}
                  onChange={(event) =>
                    setForm({ ...form, materiaId: event.target.value })
                  }
                >
                  <option value="">Seleccione una materia</option>
                  {materiasNivel.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.codigo} — {item.nombre}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Docente
                <select
                  required
                  value={form.docenteId ?? ''}
                  onChange={(event) =>
                    setForm({ ...form, docenteId: event.target.value || null })
                  }
                >
                  <option value="">Seleccione un docente</option>
                  {catalogos.docentes.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.codigoDocente ?? 'Docente institucional'}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Laboratorio
                <select
                  required
                  value={form.laboratorioId}
                  onChange={(event) =>
                    setForm({ ...form, laboratorioId: event.target.value })
                  }
                >
                  <option value="">Seleccione un laboratorio</option>
                  {catalogos.laboratorios.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.codigo} — {item.nombre}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Día
                <input
                  readOnly
                  value={
                    form.diaSemana.charAt(0) +
                    form.diaSemana.slice(1).toLowerCase()
                  }
                />
              </label>
              <label>
                Hora inicio
                <input
                  type="time"
                  min="07:30"
                  max="16:30"
                  required
                  value={form.horaInicio}
                  onChange={(event) =>
                    setForm({ ...form, horaInicio: event.target.value })
                  }
                />
              </label>
              <label>
                Hora fin
                <input
                  type="time"
                  min="08:30"
                  max="17:30"
                  required
                  value={form.horaFin}
                  onChange={(event) =>
                    setForm({ ...form, horaFin: event.target.value })
                  }
                />
              </label>
              <label className="planning-dialog__wide">
                Observación
                <textarea
                  value={form.observacion}
                  onChange={(event) =>
                    setForm({ ...form, observacion: event.target.value })
                  }
                />
              </label>
              <div className="planning-dialog__actions">
                <button type="button" onClick={() => setEditorAbierto(false)}>
                  Cancelar
                </button>
                <button disabled={guardando}>
                  {guardando ? 'Guardando...' : 'Guardar'}
                </button>
              </div>
            </form>
          </div>
        )}
        {confirmando && (
          <div
            className="planning-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="send-title"
          >
            <section>
              <h2 id="send-title">Confirmar envío</h2>
              <p>
                <strong>{catalogos.periodo?.nombre}</strong>
                <br />
                {catalogos.carrera?.nombre}
              </p>
              <dl>
                <div>
                  <dt>Materias</dt>
                  <dd>{resumen.materias}</dd>
                </div>
                <div>
                  <dt>Docentes</dt>
                  <dd>{resumen.docentes}</dd>
                </div>
                <div>
                  <dt>Laboratorios</dt>
                  <dd>{resumen.laboratorios}</dd>
                </div>
                <div>
                  <dt>Bloques planificados</dt>
                  <dd>{visibles.length}</dd>
                </div>
              </dl>
              <p>
                Todos los bloques del borrador se enviarán juntos para revisión.
              </p>
              <div className="planning-dialog__actions">
                <button onClick={() => setConfirmando(false)}>Cancelar</button>
                <button
                  onClick={() => void enviarCompleta()}
                  disabled={enviando}
                >
                  {enviando ? 'Enviando...' : 'Confirmar envío'}
                </button>
              </div>
            </section>
          </div>
        )}
      </main>
    </DashboardLayout>
  )
}
