import { useEffect, useMemo, useRef, useState } from 'react'
import { useAcademicPeriod } from '../academicPeriodContext'
import { etiquetaPeriodo } from '../academicPeriodHelpers'
import './AcademicPeriodSelector.css'

export function AcademicPeriodSelector() {
  const {
    periodos,
    periodoVigente,
    periodoSeleccionado,
    seleccionarPeriodo,
    cargando,
  } = useAcademicPeriod()

  const [abierto, setAbierto] = useState(false)
  const [busqueda, setBusqueda] = useState('')
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!abierto) return undefined
    const alHacerClicFuera = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setAbierto(false)
      }
    }
    document.addEventListener('mousedown', alHacerClicFuera)
    return () => document.removeEventListener('mousedown', alHacerClicFuera)
  }, [abierto])

  const periodoActivo = periodoSeleccionado ?? periodoVigente

  const textoBoton = cargando
    ? 'Cargando período…'
    : periodoActivo
      ? etiquetaPeriodo(periodoActivo)
      : 'Sin período académico actual'

  const periodosFiltrados = useMemo(() => {
    const termino = busqueda.trim().toLowerCase()
    if (!termino) return periodos
    return periodos.filter((p) => {
      const matchNombre = p.nombre?.toLowerCase().includes(termino)
      const matchCodigo = p.codigo?.toLowerCase().includes(termino)
      const matchPpa = p.ppaNombre?.toLowerCase().includes(termino)
      const matchEtiqueta = etiquetaPeriodo(p).toLowerCase().includes(termino)
      return matchNombre || matchCodigo || matchPpa || matchEtiqueta
    })
  }, [periodos, busqueda])

  return (
    <div className="academic-period-selector" ref={containerRef}>
      <button
        type="button"
        className="academic-period-selector__trigger"
        aria-haspopup="dialog"
        aria-expanded={abierto}
        aria-label={`Período académico: ${textoBoton}`}
        onClick={() => setAbierto((prev) => !prev)}
      >
        <span className="academic-period-selector__icon" aria-hidden="true">
          📅
        </span>
        <span className="academic-period-selector__label">{textoBoton}</span>
        <span className="academic-period-selector__arrow" aria-hidden="true">
          {abierto ? '▲' : '▼'}
        </span>
      </button>

      {abierto && (
        <div
          className="academic-period-panel"
          role="dialog"
          aria-label="Período académico"
        >
          <header className="academic-period-panel__header">
            <div className="academic-period-panel__title-wrapper">
              <span className="academic-period-panel__header-icon" aria-hidden="true">
                📅
              </span>
              <h3 className="academic-period-panel__title">PERÍODO ACADÉMICO</h3>
            </div>
            <button
              type="button"
              className="academic-period-panel__close"
              aria-label="Cerrar"
              onClick={() => setAbierto(false)}
            >
              ✕
            </button>
          </header>

          <div className="academic-period-panel__search-wrapper">
            <input
              type="text"
              className="academic-period-panel__search-input"
              placeholder="Buscar período..."
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              aria-label="Buscar período"
            />
          </div>

          <div className="academic-period-panel__subheading">
            <span>PERÍODOS DISPONIBLES</span>
          </div>

          <div className="academic-period-panel__body">
            {periodos.length === 0 ? (
              <div className="academic-period-panel__empty">
                <p>No hay períodos académicos disponibles</p>
              </div>
            ) : periodosFiltrados.length === 0 ? (
              <div className="academic-period-panel__empty">
                <p>No se encontraron períodos para la búsqueda</p>
              </div>
            ) : (
              <div
                className="academic-period-panel__list"
                role="listbox"
                aria-label="Períodos académicos disponibles"
              >
                {periodosFiltrados.map((item) => {
                  const esSeleccionado = periodoActivo?.id === item.id
                  return (
                    <button
                      key={item.id}
                      type="button"
                      role="option"
                      aria-selected={esSeleccionado}
                      className={`academic-period-panel__item ${
                        esSeleccionado ? 'is-selected' : ''
                      }`}
                      onClick={() => {
                        seleccionarPeriodo(item.id)
                        setAbierto(false)
                      }}
                    >
                      <div className="academic-period-panel__item-info">
                        <strong className="academic-period-panel__item-name">
                          {etiquetaPeriodo(item)}
                        </strong>
                        <div className="academic-period-panel__item-meta">
                          <span className="academic-period-panel__item-codigo">
                            {item.codigo}
                          </span>
                          {item.estado && (
                            <span
                              className={`academic-period-panel__badge academic-period-panel__badge--${item.estado.toLowerCase()}`}
                            >
                              {item.estado}
                            </span>
                          )}
                        </div>
                      </div>
                      {esSeleccionado && (
                        <span
                          className="academic-period-panel__check"
                          aria-label="Seleccionado"
                        >
                          ✓
                        </span>
                      )}
                    </button>
                  )
                })}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
