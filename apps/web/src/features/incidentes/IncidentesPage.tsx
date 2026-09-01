import { FormEvent, useCallback, useEffect, useState } from 'react'
import { DashboardLayout } from '../../components/DashboardLayout'
import { hasPermission, useAuth } from '../../auth'
import * as api from '../../services/operationalApi'
import '../operaciones/Operations.css'
export function IncidentesPage() {
  const { usuario } = useAuth(),
    gestor = hasPermission(usuario, 'INCIDENTE_GESTIONAR'),
    puedeCrear = hasPermission(usuario, 'INCIDENTE_CREAR')
  const [items, setItems] = useState<api.Incidente[]>([]),
    [form, setForm] = useState({
      laboratorioEquipo: '',
      descripcion: '',
      prioridad: 'MEDIA',
      fecha: new Date().toISOString().slice(0, 10),
    }),
    [error, setError] = useState(''),
    [mensaje, setMensaje] = useState(''),
    [loading, setLoading] = useState(true)
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
  }, [cargar])
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
    if (!confirm(`Â¿Cambiar el incidente a ${value}?`)) return
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
            <h1>Incidentes</h1>
            <p>Seguimiento operacional dentro de su Ã¡mbito autorizado.</p>
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
              <input
                required
                value={form.laboratorioEquipo}
                onChange={(e) =>
                  setForm({ ...form, laboratorioEquipo: e.target.value })
                }
              />
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
              DescripciÃ³n
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
        <div className="operations__table-wrap">
          {loading ? (
            <p>Cargandoâ€¦</p>
          ) : items.length === 0 ? (
            <p className="operations__empty">No existen incidentes.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Recurso</th>
                  <th>DescripciÃ³n</th>
                  <th>Prioridad</th>
                  <th>Estado</th>
                  <th>AcciÃ³n</th>
                </tr>
              </thead>
              <tbody>
                {items.map((x) => (
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
