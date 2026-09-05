import { FormEvent, useCallback, useEffect, useState } from 'react'
import { DashboardLayout } from '../../components/DashboardLayout'
import { hasPermission, hasRole, useAuth } from '../../auth'
import * as api from '../../services/operationalApi'
import { obtenerLaboratorios, type Laboratorio } from '../../services/academicoApi'
import '../operaciones/Operations.css'
export function IncidentesPage() {
  const { usuario } = useAuth(),
    gestor = hasPermission(usuario, 'INCIDENTE_GESTIONAR'),
    puedeCrear = hasPermission(usuario, 'INCIDENTE_CREAR'),
    administradorPiso = hasRole(usuario, 'ADMINISTRADOR_PISO'),
    administrador = hasRole(usuario, 'ADMINISTRADOR')
  const [items, setItems] = useState<api.Incidente[]>([]),
    [laboratorios, setLaboratorios] = useState<Laboratorio[]>([]),
    [form, setForm] = useState({
      laboratorioEquipo: '',
      descripcion: '',
      prioridad: 'MEDIA',
      fecha: new Date().toISOString().slice(0, 10),
    }),
    [error, setError] = useState(''),
    [mensaje, setMensaje] = useState(''),
    [loading, setLoading] = useState(true)
  const [filtros, setFiltros] = useState({ laboratorio: '', prioridad: '', estado: '' })
  const cargar = useCallback(async () => {
    setLoading(true)
    try {
      setItems(await api.listarIncidentes())
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo cargar.')
    } finally {
      setLoading(false)
    }
  }, [])
  useEffect(() => {
    void cargar()
    if (administradorPiso || administrador) {
      void Promise.resolve(obtenerLaboratorios())
        .then((items) => setLaboratorios(items ?? []))
        .catch(() => setLaboratorios([]))
    }
  }, [administrador, administradorPiso, cargar])
  const visibles = items.filter((item) =>
    (!filtros.laboratorio || item.laboratorioEquipo === filtros.laboratorio)
    && (!filtros.prioridad || item.prioridad === filtros.prioridad)
    && (!filtros.estado || item.estado === filtros.estado))
  async function crear(e: FormEvent) {
    e.preventDefault()
    try {
      await api.crearIncidente(form)
      setMensaje('Incidente reportado.')
      setForm({ ...form, laboratorioEquipo: '', descripcion: '' })
      await cargar()
    } catch (x) {
      setError(x instanceof Error ? x.message : 'No se pudo crear.')
    }
  }
  async function estado(id: string, value: string) {
    if (!confirm(`¿Cambiar el incidente a ${value}?`)) return
    try {
      await api.actualizarIncidente(id, value)
      await cargar()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo actualizar.')
    }
  }
  return (
    <DashboardLayout breadcrumb="Incidentes">
      <div className="operations">
        <header>
          <div>
            <h1>{administrador ? 'Supervisión global de incidentes' : 'Incidentes'}</h1>
            <p>{administrador ? 'Incidentes asociados al catálogo institucional de laboratorios.' : 'Seguimiento operacional dentro de su ámbito autorizado.'}</p>
          </div>
          <button onClick={() => void cargar()}>Actualizar</button>
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
        {puedeCrear && (
          <form className="operations__form" onSubmit={crear}>
            <label>
              Laboratorio o equipo
              {administradorPiso || administrador ? (
                <select
                  required
                  value={form.laboratorioEquipo}
                  onChange={(e) =>
                    setForm({ ...form, laboratorioEquipo: e.target.value })
                  }
                >
                  <option value="">Seleccione un laboratorio</option>
                  {laboratorios.map((laboratorio) => (
                    <option key={laboratorio.id} value={laboratorio.codigo}>
                      {laboratorio.codigo} — {laboratorio.nombre}
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  required
                  value={form.laboratorioEquipo}
                  onChange={(e) =>
                    setForm({ ...form, laboratorioEquipo: e.target.value })
                  }
                />
              )}
            </label>
            <label>
              Prioridad
              <select
                value={form.prioridad}
                onChange={(e) =>
                  setForm({ ...form, prioridad: e.target.value })
                }
              >
                <option>BAJA</option>
                <option>MEDIA</option>
                <option>ALTA</option>
              </select>
            </label>
            <label className="operations__wide">
              Descripción
              <textarea
                required
                value={form.descripcion}
                onChange={(e) =>
                  setForm({ ...form, descripcion: e.target.value })
                }
              />
            </label>
            <button>Reportar incidente</button>
          </form>
        )}
        {administrador && <div className="operations__form">
          <label>Laboratorio<select value={filtros.laboratorio} onChange={(e) => setFiltros({ ...filtros, laboratorio: e.target.value })}><option value="">Todos</option>{laboratorios.map((lab) => <option key={lab.id} value={lab.codigo}>{lab.codigo} — {lab.nombre}</option>)}</select></label>
          <label>Prioridad<select value={filtros.prioridad} onChange={(e) => setFiltros({ ...filtros, prioridad: e.target.value })}><option value="">Todas</option><option>BAJA</option><option>MEDIA</option><option>ALTA</option></select></label>
          <label>Estado<select value={filtros.estado} onChange={(e) => setFiltros({ ...filtros, estado: e.target.value })}><option value="">Todos</option><option>REPORTADO</option><option>EN_REVISION</option><option>RESUELTO</option></select></label>
        </div>}
        <div className="operations__table-wrap">
          {loading ? (
            <p>Cargando…</p>
          ) : visibles.length === 0 ? (
            <p className="operations__empty">No existen incidentes.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Recurso</th>
                  <th>Descripción</th>
                  <th>Prioridad</th>
                  <th>Estado</th>
                  <th>Acción</th>
                </tr>
              </thead>
              <tbody>
                {visibles.map((x) => (
                  <tr key={x.id}>
                    <td>{x.laboratorioEquipo}</td>
                    <td>{x.descripcion}</td>
                    <td>{x.prioridad}</td>
                    <td>
                      <span className="status">{x.estado}</span>
                    </td>
                    <td>
                      {gestor && (
                        <select
                          aria-label={`Estado de ${x.laboratorioEquipo}`}
                          value={x.estado}
                          onChange={(e) => void estado(x.id, e.target.value)}
                        >
                          <option>REPORTADO</option>
                          <option>EN_REVISION</option>
                          <option>RESUELTO</option>
                        </select>
                      )}
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
