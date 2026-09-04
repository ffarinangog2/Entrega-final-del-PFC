import { useEffect, useMemo, useState } from 'react'
import { DashboardLayout } from '../../components/DashboardLayout'
import {
  obtenerCarreras,
  obtenerLaboratorios,
  obtenerMaterias,
  obtenerPeriodos,
  obtenerPisos,
  type Carrera,
  type Laboratorio,
  type Materia,
  type PeriodoLectivo,
  type Piso,
} from '../../services/academicoApi'
import {
  obtenerMiHorario,
  historialAsistencia,
  type Planificacion,
  type RegistroAsistencia,
} from '../../services/operationalApi'
import {
  obtenerDocenteResumen,
  obtenerMisContextosAcademicos,
  type ContextoAcademicoEstudiante,
} from '../../services/usuariosApi'
import '../operaciones/Operations.css'

type Mode = 'horario' | 'laboratorios' | 'historial'
const DIAS = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES']
const HORAS = Array.from({ length: 10 }, (_, indice) => `${String(indice + 7).padStart(2, '0')}:30`)
const minutos = (hora: string) => {
  const [h, m] = hora.split(':').map(Number)
  return h * 60 + m
}

export function StudentAcademicPage({ mode }: { mode: Mode }) {
  const [contextos, setContextos] = useState<ContextoAcademicoEstudiante[]>([])
  const [seleccion, setSeleccion] = useState('')
  const [horario, setHorario] = useState<Planificacion[]>([])
  const [asistencias, setAsistencias] = useState<RegistroAsistencia[]>([])
  const [materias, setMaterias] = useState<Materia[]>([])
  const [labs, setLabs] = useState<Laboratorio[]>([])
  const [pisos, setPisos] = useState<Piso[]>([])
  const [carreras, setCarreras] = useState<Carrera[]>([])
  const [periodos, setPeriodos] = useState<PeriodoLectivo[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [detalle, setDetalle] = useState<Planificacion | null>(null)
  const [docentes, setDocentes] = useState<Map<string, string>>(new Map())
  useEffect(() => {
    let activo = true
    Promise.all([
      obtenerMisContextosAcademicos(),
      obtenerMaterias(),
      obtenerLaboratorios(),
      obtenerPisos(),
      obtenerCarreras(),
      obtenerPeriodos(),
    ])
      .then(([ctx, mat, lab, piso, carr, periodo]) => {
        if (!activo) return
        setContextos(ctx)
        setSeleccion(
          ctx.find((x) => x.activo)?.periodoId ?? ctx[0]?.periodoId ?? '',
        )
        setMaterias(mat)
        setLabs(lab)
        setPisos(piso)
        setCarreras(carr)
        setPeriodos(periodo)
      })
      .catch(
        (e) =>
          activo &&
          setError(
            e instanceof Error
              ? e.message
              : 'No se pudo cargar la información académica.',
          ),
      )
      .finally(() => activo && setCargando(false))
    return () => {
      activo = false
    }
  }, [])
  useEffect(() => {
    if (!seleccion) return
    let activo = true
    setCargando(true)
    Promise.all([obtenerMiHorario(seleccion), mode === 'historial' ? historialAsistencia(seleccion) : Promise.resolve([])])
      .then(([bloques, registros]) => { if (activo) { setHorario(bloques); setAsistencias(registros) } })
      .catch(
        (e) =>
          activo &&
          setError(
            e instanceof Error ? e.message : 'No se pudo cargar el horario.',
          ),
      )
      .finally(() => activo && setCargando(false))
    return () => {
      activo = false
    }
  }, [seleccion, mode])
  useEffect(() => {
    const ids = [...new Set(horario.map((item) => item.docenteId).filter((id): id is string => Boolean(id)))]
    if (ids.length === 0) { setDocentes(new Map()); return }
    let activo = true
    void Promise.all(ids.map(obtenerDocenteResumen))
      .then((items) => { if (activo) setDocentes(new Map(items.map((item) => [item.id, `${item.nombres} ${item.apellidos}`]))) })
      .catch(() => { if (activo) setDocentes(new Map()) })
    return () => { activo = false }
  }, [horario])
  const contexto = contextos.find((x) => x.periodoId === seleccion)
  const mat = useMemo(() => new Map(materias.map((x) => [x.id, x])), [materias])
  const lab = useMemo(() => new Map(labs.map((x) => [x.id, x])), [labs])
  const piso = useMemo(() => new Map(pisos.map((x) => [x.id, x])), [pisos])
  const title =
    mode === 'horario'
      ? 'Mi horario'
      : mode === 'laboratorios'
        ? 'Laboratorios de mi horario'
        : 'Historial académico'
  const laboratoriosHorario = [...new Set(horario.map((x) => x.laboratorioId))]
    .map((id) => lab.get(id))
    .filter((x): x is Laboratorio => Boolean(x))
  return (
    <DashboardLayout breadcrumb={title}>
      <div className="operations">
        <header>
          <div>
            <h1>{title}</h1>
            <p>
              Información derivada de la planificación aprobada de tu carrera,
              nivel y ciclo.
            </p>
          </div>
        </header>
        {error && (
          <p role="alert" className="operations__error">
            {error}
          </p>
        )}
        {contextos.length > 0 && (
          <label>
            Ciclo consultado
            <select
              value={seleccion}
              onChange={(e) => setSeleccion(e.target.value)}
            >
              {contextos.map((c) => (
                <option key={c.id} value={c.periodoId}>
                  {periodos.find((p) => p.id === c.periodoId)?.nombre ??
                    'Ciclo académico'}{' '}
                  · Nivel {c.nivel}
                  {c.activo ? ' · Actual' : ''}
                </option>
              ))}
            </select>
          </label>
        )}
        {contexto && (
          <p>
            <strong>
              {carreras.find((c) => c.id === contexto.carreraId)?.nombre ??
                'Carrera institucional'}
            </strong>{' '}
            · Nivel {contexto.nivel}
          </p>
        )}
        {cargando ? (
          <p role="status">Cargando...</p>
        ) : mode === 'laboratorios' ? (
          <div className="operations__cards">
            {laboratoriosHorario.length === 0 ? (
              <p className="operations__empty">
                No hay laboratorios vinculados a tu horario aprobado.
              </p>
            ) : (
              laboratoriosHorario.map((l) => (
                <article className="operations__card" key={l.id}>
                  <h2>
                    {l.codigo} — {l.nombre}
                  </h2>
                  <p>Piso {piso.get(l.pisoId)?.numero ?? 'institucional'}</p>
                  <p>
                    {l.estado} · Capacidad {l.capacidad}
                  </p>
                </article>
              ))
            )}
          </div>
        ) : horario.length === 0 ? (
          <p className="operations__empty">
            No existe un horario aprobado para el ciclo seleccionado.
          </p>
        ) : (
          <>{mode === 'historial' && <section className="operations__card"><h2>Mis registros de uso</h2>{asistencias.length === 0 ? <p>Aún no tienes registros en este ciclo.</p> : asistencias.map((registro) => { const bloque = horario.find((item) => item.id === registro.bloqueId); return <p key={registro.id}><strong>{new Date(registro.registradaEn).toLocaleString()}</strong> · {mat.get(bloque?.materiaId ?? '')?.nombre ?? 'Actividad de laboratorio'} · {registro.estado}</p> })}</section>}
          <div className="operations__table-wrap">
            <table aria-label="Horario semanal de laboratorio">
              <thead><tr><th>Hora</th>{DIAS.map((dia) => <th key={dia}>{dia[0] + dia.slice(1).toLowerCase()}</th>)}</tr></thead>
              <tbody>
                {HORAS.map((hora) => <tr key={hora}>
                  <th>{hora}</th>
                  {DIAS.map((dia) => {
                    const inicioCelda = minutos(hora)
                    const bloque = horario.find((item) => item.diaSemana === dia && minutos(item.horaInicio) >= inicioCelda && minutos(item.horaInicio) < inicioCelda + 60)
                    return <td key={`${dia}-${hora}`}>
                      {bloque && <button className="student-schedule__block" onClick={() => setDetalle(bloque)}>
                        <strong>{bloque.horaInicio}–{bloque.horaFin}</strong>
                        <span>{mat.get(bloque.materiaId)?.nombre ?? 'Materia planificada'}</span>
                        <span>{lab.get(bloque.laboratorioId)?.codigo ?? 'Laboratorio'} · Piso {piso.get(lab.get(bloque.laboratorioId)?.pisoId ?? '')?.numero ?? '—'}</span>
                        <span>{bloque.docenteId ? docentes.get(bloque.docenteId) ?? 'Docente asignado' : 'Docente por confirmar'}</span>
                      </button>}
                    </td>
                  })}
                </tr>)}
              </tbody>
            </table>
          </div></>
        )}
        {detalle && (
          <section className="operations__card" aria-label="Detalle de clase">
            <h2>{mat.get(detalle.materiaId)?.nombre}</h2>
            <p>
              {detalle.diaSemana} · {detalle.horaInicio}–{detalle.horaFin}
            </p>
            <p>
              {lab.get(detalle.laboratorioId)?.nombre} · Piso{' '}
              {piso.get(lab.get(detalle.laboratorioId)?.pisoId ?? '')?.numero}
            </p>
            <p>Solo lectura</p>
            <button onClick={() => setDetalle(null)}>Cerrar</button>
          </section>
        )}
      </div>
    </DashboardLayout>
  )
}
export const MiHorarioPage = () => <StudentAcademicPage mode="horario" />
export const StudentLaboratoriosPage = () => (
  <StudentAcademicPage mode="laboratorios" />
)
export const StudentHistorialPage = () => (
  <StudentAcademicPage mode="historial" />
)
