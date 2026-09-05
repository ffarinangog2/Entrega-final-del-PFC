import { useMemo, useState } from 'react'
import { estadoEfectivo, etiquetaPeriodo, useAcademicPeriod } from '../academicPeriod'

export function AcademicPeriodSelector() {
  const { periodos, periodoVigente, periodoSeleccionado, seleccionarPeriodo, cargando } = useAcademicPeriod()
  const [buscar, setBuscar] = useState('')
  const visibles = useMemo(() => periodos.filter((p) => etiquetaPeriodo(p).toLowerCase().includes(buscar.toLowerCase())), [buscar, periodos])
  return <details className="academic-period-selector">
    <summary>{cargando ? 'Cargando período…' : periodoSeleccionado ? etiquetaPeriodo(periodoSeleccionado) : 'Período académico'}</summary>
    <div className="academic-period-selector__menu">
      <strong>PERÍODO ACADÉMICO</strong>
      <input aria-label="Buscar período" placeholder="Buscar período…" value={buscar} onChange={(e) => setBuscar(e.target.value)} />
      {visibles.map((periodo) => <button key={periodo.id} className={periodo.id === periodoSeleccionado?.id ? 'is-selected' : ''} onClick={() => seleccionarPeriodo(periodo.id)}>
        <span>{periodo.id === periodoSeleccionado?.id ? '✓ ' : ''}{etiquetaPeriodo(periodo)}</span>
        <small>{estadoEfectivo(periodo, periodoVigente?.id)}</small>
      </button>)}
    </div>
  </details>
}
