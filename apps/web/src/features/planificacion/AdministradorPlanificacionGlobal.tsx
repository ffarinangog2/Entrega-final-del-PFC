import { useEffect, useMemo, useState } from 'react'
import { DashboardLayout } from '../../components/DashboardLayout'
import { obtenerCarreras, obtenerLaboratorios, obtenerMaterias, obtenerPeriodos, obtenerPisos, type Carrera, type Laboratorio, type Materia, type PeriodoLectivo, type Piso } from '../../services/academicoApi'
import { listarPlanificacionesAgregadas, type PlanificacionAgregada } from '../../services/operationalApi'
import '../operaciones/Operations.css'

export function AdministradorPlanificacionGlobal() {
  const [planes, setPlanes] = useState<PlanificacionAgregada[]>([])
  const [carreras, setCarreras] = useState<Carrera[]>([])
  const [periodos, setPeriodos] = useState<PeriodoLectivo[]>([])
  const [materias, setMaterias] = useState<Materia[]>([])
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([])
  const [pisos, setPisos] = useState<Piso[]>([])
  const [filtros, setFiltros] = useState({ carrera: '', ciclo: '', estado: '', piso: '', nivel: '' })
  const [seleccionado, setSeleccionado] = useState('')
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => {
    Promise.all([listarPlanificacionesAgregadas(), obtenerCarreras(), obtenerPeriodos(), obtenerMaterias(), obtenerLaboratorios(), obtenerPisos()])
      .then(([p, c, pe, m, l, pi]) => { setPlanes(p); setCarreras(c); setPeriodos(pe); setMaterias(m); setLaboratorios(l); setPisos(pi) })
      .catch((cause) => setError(cause instanceof Error ? cause.message : 'No fue posible cargar la supervisión global.'))
      .finally(() => setCargando(false))
  }, [])
  const labPorId = useMemo(() => new Map(laboratorios.map((item) => [item.id, item])), [laboratorios])
  const visibles = planes.filter((plan) => {
    const bloques = plan.bloques.filter((bloque) => (!filtros.piso || labPorId.get(bloque.laboratorioId)?.pisoId === filtros.piso) && (!filtros.nivel || bloque.nivel === Number(filtros.nivel)))
    return (!filtros.carrera || plan.carreraId === filtros.carrera) && (!filtros.ciclo || plan.periodoId === filtros.ciclo) && (!filtros.estado || plan.estado === filtros.estado) && ((!filtros.piso && !filtros.nivel) || bloques.length > 0)
  })
  const actual = planes.find((item) => item.id === seleccionado)
  const nombrePiso = (id: string) => `Piso ${pisos.find((piso) => piso.id === id)?.numero ?? '—'}`
  return <DashboardLayout breadcrumb="Supervisión de planificación"><section className="operations">
    <header><div><h1>Planificación académica global</h1><p>Planes agregados por carrera, PPA y ciclo académico.</p></div></header>
    {cargando && <p role="status">Cargando planes...</p>}{error && <p role="alert" className="operations__error">{error}</p>}
    {!cargando && !error && <><div className="operations__form">
      <label>Carrera<select value={filtros.carrera} onChange={(e) => setFiltros({ ...filtros, carrera: e.target.value })}><option value="">Todas</option>{carreras.map((x) => <option key={x.id} value={x.id}>{x.nombre}</option>)}</select></label>
      <label>Ciclo académico<select value={filtros.ciclo} onChange={(e) => setFiltros({ ...filtros, ciclo: e.target.value })}><option value="">Todos</option>{periodos.map((x) => <option key={x.id} value={x.id}>{x.ppaNombre ?? x.nombre} · Ciclo {x.cicloAcademico ?? 'histórico'}</option>)}</select></label>
      <label>Estado<select value={filtros.estado} onChange={(e) => setFiltros({ ...filtros, estado: e.target.value })}><option value="">Todos</option>{['BORRADOR', 'EN_REVISION', 'REQUIERE_CAMBIOS', 'APROBADA', 'FINALIZADA'].map((x) => <option key={x}>{x}</option>)}</select></label>
      <label>Piso<select value={filtros.piso} onChange={(e) => setFiltros({ ...filtros, piso: e.target.value })}><option value="">Todos</option>{pisos.map((x) => <option key={x.id} value={x.id}>Piso {x.numero}</option>)}</select></label>
      <label>Nivel<select value={filtros.nivel} onChange={(e) => setFiltros({ ...filtros, nivel: e.target.value })}><option value="">Todos</option>{Array.from({ length: 10 }, (_, i) => i + 1).map((x) => <option key={x}>{x}</option>)}</select></label>
    </div>{visibles.length === 0 ? <p className="operations__empty">No existen planes con estos filtros.</p> : <div className="operations__cards">{visibles.map((plan) => { const periodo = periodos.find((x) => x.id === plan.periodoId); const pisosPlan = [...new Set(plan.bloques.map((x) => labPorId.get(x.laboratorioId)?.pisoId).filter(Boolean))] as string[]; return <article className="operations__card" key={plan.id}><h2>{carreras.find((x) => x.id === plan.carreraId)?.nombre ?? 'Carrera'}</h2><p>{periodo?.ppaNombre ?? periodo?.nombre ?? 'PPA'} · Ciclo {periodo?.cicloAcademico ?? '—'}</p><p><strong>{plan.estado.replace(/_/g, ' ')}</strong> · {plan.bloques.length} bloques · {new Set(plan.bloques.map((x) => x.nivel)).size} niveles</p><p>{pisosPlan.map(nombrePiso).join(' · ') || 'Sin pisos involucrados'}</p><p>{plan.revisiones.map((x) => `${nombrePiso(x.pisoId)}: ${x.estado.replace(/_/g, ' ')}`).join(' · ') || 'Sin revisiones'}</p><button onClick={() => setSeleccionado(plan.id)}>Ver detalle</button></article> })}</div>}
    {actual && <section><h2>Detalle del plan</h2><div className="operations__table-wrap"><table><thead><tr><th>Nivel</th><th>Materia</th><th>Laboratorio / piso</th><th>Día</th><th>Horario</th></tr></thead><tbody>{actual.bloques.map((bloque) => { const lab = labPorId.get(bloque.laboratorioId); return <tr key={bloque.id}><td>{bloque.nivel ?? '—'}</td><td>{materias.find((x) => x.id === bloque.materiaId)?.nombre ?? 'Materia'}</td><td>{lab?.codigo ?? 'Laboratorio'} · {lab ? nombrePiso(lab.pisoId) : 'Piso —'}</td><td>{bloque.diaSemana}</td><td>{bloque.horaInicio}–{bloque.horaFin}</td></tr> })}</tbody></table></div></section>}
    </>}
  </section></DashboardLayout>
}
