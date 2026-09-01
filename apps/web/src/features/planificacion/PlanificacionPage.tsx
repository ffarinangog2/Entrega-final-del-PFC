import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react'
import { DashboardLayout } from '../../components/DashboardLayout'
import { hasPermission, hasRole, useAuth } from '../../auth'
import * as academico from '../../services/academicoApi'
import * as api from '../../services/operationalApi'
import '../operaciones/Operations.css'

const inicial: api.GuardarPlanificacion = {
  periodoId: '',
  carreraId: '',
  materiaId: '',
  docenteId: null,
  laboratorioId: '',
  diaSemana: 'LUNES',
  horaInicio: '08:00',
  horaFin: '10:00',
  observacion: '',
}
export function PlanificacionPage() {
  const { usuario } = useAuth()
  const coordinador = hasRole(usuario, 'COORDINADOR')
  const gestor = hasPermission(usuario, 'SOLICITUD_APROBAR')
  const [items, setItems] = useState<api.Planificacion[]>([]),
    [form, setForm] = useState(inicial),
    [editandoId, setEditandoId] = useState<string | null>(null),
    [cargando, setCargando] = useState(true),
    [guardando, setGuardando] = useState(false),
    [mensaje, setMensaje] = useState(''),
    [error, setError] = useState('')
  const [catalogos, setCatalogos] = useState<{
    periodo?: academico.PeriodoLectivo
    carreras: academico.Carrera[]
    materias: academico.Materia[]
    laboratorios: academico.Laboratorio[]
    docentes: academico.Docente[]
  }>({ carreras: [], materias: [], laboratorios: [], docentes: [] })
  const cargar = useCallback(async () => {
    setCargando(true)
    setError('')
    try {
      const [planes, periodo, carreras, materias, laboratorios, docentes] =
        await Promise.all([
          api.listarPlanificaciones(),
          academico.obtenerPeriodoActual(),
          coordinador ? Promise.resolve([]) : academico.obtenerCarreras(),
          academico.obtenerMaterias(),
          academico.obtenerLaboratorios(),
          academico.obtenerDocentes(),
        ])
      const carreraCoordinada = coordinador
        ? [...new Set(materias.map((materia) => materia.carreraId))]
        : []
      if (coordinador && carreraCoordinada.length !== 1) {
        throw new Error(
          'No se pudo determinar una única carrera institucional para el coordinador.',
        )
      }
      setItems(planes)
      setCatalogos({ periodo, carreras, materias, laboratorios, docentes })
      setForm((f) => ({
        ...f,
        periodoId: f.periodoId || periodo.id,
        carreraId: coordinador ? carreraCoordinada[0] : f.carreraId,
      }))
    } catch (e) {
      setError(
        e instanceof Error ? e.message : 'No se pudo cargar la planificación.',
      )
    } finally {
      setCargando(false)
    }
  }, [coordinador])
  useEffect(() => {
    void cargar()
  }, [cargar])
  const materias = useMemo(
    () =>
      catalogos.materias.filter(
        (m) => !form.carreraId || m.carreraId === form.carreraId,
      ),
    [catalogos.materias, form.carreraId],
  )
  async function guardar(e: FormEvent) {
    e.preventDefault()
    setGuardando(true)
    setError('')
    try {
      if (editandoId) await api.editarPlanificacion(editandoId, form)
      else await api.crearPlanificacion(form)
      setMensaje(
        editandoId
          ? 'Planificación actualizada correctamente.'
          : 'Borrador guardado correctamente.',
      )
      setEditandoId(null)
      setForm({ ...inicial, periodoId: catalogos.periodo?.id ?? '' })
      await cargar()
    } catch (x) {
      setError(x instanceof Error ? x.message : 'No se pudo guardar.')
    } finally {
      setGuardando(false)
    }
  }
  function editar(item: api.Planificacion) {
    setEditandoId(item.id)
    setForm({
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
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
  async function accion(
    id: string,
    a: 'enviar' | 'aceptar' | 'aceptar-propuesta' | 'reenviar' | 'cancelar',
  ) {
    if (
      (a === 'cancelar' || a === 'aceptar') &&
      !confirm(`¿Confirma la acción ${a}?`)
    )
      return
    setError('')
    try {
      await api.accionPlanificacion(id, a)
      setMensaje('Estado actualizado.')
      await cargar()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo actualizar.')
    }
  }
  async function revisar(id: string, tipo: 'rechazar' | 'proponer') {
    const observacion = prompt(
      tipo === 'rechazar'
        ? 'Motivo del rechazo'
        : 'Observación de la alternativa',
    )
    if (observacion === null) return
    try {
      if (tipo === 'rechazar') await api.rechazarPlanificacion(id, observacion)
      else await api.proponerPlanificacion(id, { observacion })
      await cargar()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo responder.')
    }
  }
  return (
    <DashboardLayout breadcrumb="Planificación semestral">
      <div className="operations">
        <header>
          <div>
            <h1>Planificación semestral</h1>
            <p>Asignaciones institucionales del periodo activo.</p>
          </div>
          <button onClick={() => void cargar()} disabled={cargando}>
            Actualizar
          </button>
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
        {coordinador && (
          <form className="operations__form" onSubmit={guardar}>
            <h2>{editandoId ? 'Editar planificación' : 'Nuevo borrador'}</h2>
            <label>
              Carrera
              <input
                readOnly
                value={
                  form.carreraId
                    ? 'Mi carrera institucional'
                    : 'Sin carrera asignada'
                }
              />
            </label>
            <label>
              Materia
              <select
                required
                value={form.materiaId}
                onChange={(e) =>
                  setForm({ ...form, materiaId: e.target.value })
                }
              >
                <option value="">Seleccione</option>
                {materias.map((x) => (
                  <option key={x.id} value={x.id}>
                    {x.codigo} — {x.nombre}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Laboratorio
              <select
                required
                value={form.laboratorioId}
                onChange={(e) =>
                  setForm({ ...form, laboratorioId: e.target.value })
                }
              >
                <option value="">Seleccione</option>
                {catalogos.laboratorios.map((x) => (
                  <option key={x.id} value={x.id}>
                    {x.nombre} — capacidad {x.capacidad}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Docente
              <select
                value={form.docenteId ?? ''}
                onChange={(e) =>
                  setForm({ ...form, docenteId: e.target.value || null })
                }
              >
                <option value="">Sin asignar</option>
                {catalogos.docentes.map((x) => (
                  <option key={x.id} value={x.id}>
                    {x.codigoDocente || 'Docente'}{' '}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Día
              <select
                value={form.diaSemana}
                onChange={(e) =>
                  setForm({ ...form, diaSemana: e.target.value })
                }
              >
                {['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES'].map(
                  (x) => (
                    <option key={x}>{x}</option>
                  ),
                )}
              </select>
            </label>
            <label>
              Inicio
              <input
                type="time"
                required
                min="07:30"
                max="17:30"
                value={form.horaInicio}
                onChange={(e) =>
                  setForm({ ...form, horaInicio: e.target.value })
                }
              />
            </label>
            <label>
              Fin
              <input
                type="time"
                required
                min="07:30"
                max="17:30"
                value={form.horaFin}
                onChange={(e) => setForm({ ...form, horaFin: e.target.value })}
              />
            </label>
            <label className="operations__wide">
              Observación
              <textarea
                value={form.observacion}
                onChange={(e) =>
                  setForm({ ...form, observacion: e.target.value })
                }
              />
            </label>
            <button disabled={guardando}>
              {guardando
                ? 'Guardando…'
                : editandoId
                  ? 'Guardar cambios'
                  : 'Guardar borrador'}
            </button>
            {editandoId && (
              <button
                type="button"
                onClick={() => {
                  setEditandoId(null)
                  setForm({
                    ...inicial,
                    periodoId: catalogos.periodo?.id ?? '',
                    carreraId: form.carreraId,
                  })
                }}
              >
                Cancelar edición
              </button>
            )}
          </form>
        )}
        <div className="operations__table-wrap">
          {cargando ? (
            <p>Cargando planificación…</p>
          ) : items.length === 0 ? (
            <p className="operations__empty">
              No existen planificaciones para su ámbito.
            </p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Materia</th>
                  <th>Día</th>
                  <th>Horario</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {items.map((x) => (
                  <tr key={x.id}>
                    <td>
                      {catalogos.materias.find((m) => m.id === x.materiaId)
                        ?.nombre || x.materiaId}
                    </td>
                    <td>{x.diaSemana}</td>
                    <td>
                      {x.horaInicio}–{x.horaFin}
                    </td>
                    <td>
                      <span
                        className={`status status--${x.estado.toLowerCase()}`}
                      >
                        {x.estado.replace(/_/g, ' ')}
                      </span>
                    </td>
                    <td>
                      <div className="operations__actions">
                        {coordinador && x.estado === 'BORRADOR' && (
                          <>
                            <button onClick={() => editar(x)}>Editar</button>
                            <button onClick={() => void accion(x.id, 'enviar')}>
                              Enviar
                            </button>
                          </>
                        )}
                        {coordinador && x.estado === 'PROPUESTA_CAMBIO' && (
                          <>
                            <button onClick={() => editar(x)}>
                              Editar alternativa
                            </button>
                            <button
                              onClick={() =>
                                void accion(x.id, 'aceptar-propuesta')
                              }
                            >
                              Aceptar propuesta
                            </button>
                            <button
                              onClick={() => void accion(x.id, 'reenviar')}
                            >
                              Reenviar
                            </button>
                          </>
                        )}
                        {coordinador &&
                          ['BORRADOR', 'ENVIADA', 'PROPUESTA_CAMBIO'].includes(
                            x.estado,
                          ) && (
                            <button
                              className="danger"
                              onClick={() => void accion(x.id, 'cancelar')}
                            >
                              Cancelar
                            </button>
                          )}
                        {gestor && x.estado === 'ENVIADA' && (
                          <>
                            <button
                              onClick={() => void accion(x.id, 'aceptar')}
                            >
                              Aceptar
                            </button>
                            <button
                              onClick={() => void revisar(x.id, 'proponer')}
                            >
                              Alternativa
                            </button>
                            <button
                              className="danger"
                              onClick={() => void revisar(x.id, 'rechazar')}
                            >
                              Rechazar
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </DashboardLayout>
  )
}
