import { useAcademicPeriod } from '../academicPeriodContext'
import { etiquetaPeriodo } from '../academicPeriodHelpers'

export function AcademicPeriodSelector() {
  const { periodoVigente, cargando } = useAcademicPeriod()
  return <div className="academic-period-selector" aria-label="Período académico actual">
    {cargando ? 'Cargando período…' : periodoVigente ? etiquetaPeriodo(periodoVigente) : 'Sin período académico actual'}
  </div>
}
