import { useEffect, useState } from 'react'
import { obtenerLaboratorios, type Laboratorio, type EstadoLaboratorio } from '../../services/academicoApi'
import './LaboratoriosPanel.css'

const ESTADO_LABEL: Record<EstadoLaboratorio, string> = {
  DISPONIBLE: 'Disponible',
  OCUPADO: 'Ocupado',
  MANTENIMIENTO: 'Mantenimiento',
  INACTIVO: 'Inactivo',
}

export function LaboratoriosPanel() {
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    obtenerLaboratorios()
      .then(setLaboratorios)
      .catch((err: Error) => setError(err.message))
      .finally(() => setCargando(false))
  }, [])

  const conteoPorEstado = laboratorios.reduce<Record<string, number>>((acc, lab) => {
    acc[lab.estado] = (acc[lab.estado] ?? 0) + 1
    return acc
  }, {})

  if (cargando) {
    return (
      <div className="labs-panel">
        <div className="labs-panel__banner">SCLI · Laboratorios Informaticos</div>
        <p className="labs-panel__status-text">Cargando laboratorios...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="labs-panel">
        <div className="labs-panel__banner">SCLI · Laboratorios Informaticos</div>
        <p className="labs-panel__status-text labs-panel__status-text--error">
          No se pudo conectar con el servicio academico: {error}
        </p>
      </div>
    )
  }

  return (
    <div className="labs-panel">
      <div className="labs-panel__banner">SCLI · Laboratorios Informaticos</div>
      <div className="labs-panel__content">
        <header className="labs-panel__header">
          <h1 className="labs-panel__title">Monitoreo de laboratorios</h1>
          <div className="labs-panel__summary">
            <span className="labs-panel__summary-item labs-panel__summary-item--disponible">
              {conteoPorEstado.DISPONIBLE ?? 0} disponibles
            </span>
            <span className="labs-panel__summary-item">
              {laboratorios.length} en total
            </span>
          </div>
        </header>

        <div className="labs-panel__grid">
          {laboratorios.map((lab) => (
            <article key={lab.id} className={`labs-card labs-card--${lab.estado.toLowerCase()}`}>
              <div className="labs-card__top">
                <span className="labs-card__codigo">{lab.codigo}</span>
                <span className="labs-card__estado-dot" />
              </div>
              <h2 className="labs-card__nombre">{lab.nombre}</h2>
              <p className="labs-card__estado-label">{ESTADO_LABEL[lab.estado]}</p>
              <div className="labs-card__capacidad-row">
                <span>Capacidad</span>
                <span>{lab.capacidad}</span>
              </div>
              <div className="labs-card__capacidad-bar">
                <div
                  className="labs-card__capacidad-fill"
                  style={{ width: `${Math.min((lab.capacidad / 40) * 100, 100)}%` }}
                />
              </div>
            </article>
          ))}
        </div>

        {laboratorios.length === 0 && (
          <p className="labs-panel__status-text">No hay laboratorios registrados todavia.</p>
        )}
      </div>
    </div>
  )
}