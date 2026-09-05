import { useEffect, useState, type ReactNode } from 'react'
import { useAcademicPeriod } from '../../academicPeriodContext'
import { etiquetaPeriodo } from '../../academicPeriodHelpers'
import { obtenerCarreras, type Carrera } from '../../services/academicoApi'
import { confirmarMiContextoAcademico, obtenerMisContextosAcademicos } from '../../services/usuariosApi'

export function StudentContextGate({ children }: { children: ReactNode }) {
  const { periodoVigente } = useAcademicPeriod()
  const [pendiente, setPendiente] = useState(false)
  const [carreras, setCarreras] = useState<Carrera[]>([])
  const [carreraId, setCarreraId] = useState('')
  const [nivel, setNivel] = useState(1)
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)
  useEffect(() => {
    if (!periodoVigente) return
    Promise.all([obtenerMisContextosAcademicos(), obtenerCarreras()]).then(([contextos, lista]) => {
      setCarreras(lista.filter((item) => item.activo))
      setCarreraId(lista.find((item) => item.activo)?.id ?? '')
      setPendiente(!contextos.some((item) => item.periodoId === periodoVigente.id))
    }).catch((cause) => setError(cause instanceof Error ? cause.message : 'No se pudo verificar el contexto académico.'))
  }, [periodoVigente])
  if (!pendiente) return <>{children}</>
  async function guardar() {
    if (!periodoVigente || !carreraId) return
    setGuardando(true); setError('')
    try { await confirmarMiContextoAcademico({ carreraId, periodoId: periodoVigente.id, nivel }); setPendiente(false) }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo guardar el contexto académico.') }
    finally { setGuardando(false) }
  }
  return <section className="student-context-gate">
    <h1>Confirma tu contexto académico</h1>
    <label>Período<input readOnly value={periodoVigente ? etiquetaPeriodo(periodoVigente) : ''} /></label>
    <label>Carrera<select value={carreraId} onChange={(e) => setCarreraId(e.target.value)}>{carreras.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label>
    <label>Nivel actual<select value={nivel} onChange={(e) => setNivel(Number(e.target.value))}>{Array.from({ length: 10 }, (_, i) => i + 1).map((value) => <option key={value} value={value}>{value}°</option>)}</select></label>
    <p>Esta información se utiliza únicamente dentro de SCLI para asociar horarios y actividades de laboratorio. No constituye una matrícula académica oficial.</p>
    {error && <p role="alert" className="operations__error">{error}</p>}
    <button onClick={() => void guardar()} disabled={guardando || !carreraId}>{guardando ? 'Guardando…' : 'Guardar y continuar'}</button>
  </section>
}
