import { useEffect, useState } from 'react'
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts'
import { obtenerOcupacionHistorica, type SerieEstado, type EstadoLaboratorio } from '../../services/academicoApi'
import './MonitoreoPanel.css'

const ESTADO_LABEL: Record<EstadoLaboratorio, string> = {
  DISPONIBLE: 'Disponible',
  OCUPADO: 'Ocupado',
  MANTENIMIENTO: 'Mantenimiento',
  INACTIVO: 'Inactivo',
}

const ESTADO_COLOR: Record<EstadoLaboratorio, string> = {
  DISPONIBLE: '#2e7d32',
  OCUPADO: '#f57c00',
  MANTENIMIENTO: '#d84315',
  INACTIVO: '#9e9e9e',
}

interface PuntoGrafico {
  instante: string
  [estado: string]: string | number
}

function aFormatoGrafico(series: SerieEstado[]): PuntoGrafico[] {
  const puntosPorInstante = new Map<string, PuntoGrafico>()

  for (const serie of series) {
    for (const punto of serie.puntos) {
      const clave = punto.instante
      const existente = puntosPorInstante.get(clave) ?? { instante: clave }
      existente[serie.estado] = punto.valor
      puntosPorInstante.set(clave, existente)
    }
  }

  return Array.from(puntosPorInstante.values()).sort((a, b) => a.instante.localeCompare(b.instante))
}

function formatearHora(instante: unknown): string {
  return new Date(String(instante)).toLocaleTimeString('es-EC', { hour: '2-digit', minute: '2-digit' })
}

export function MonitoreoPanel() {
  const [series, setSeries] = useState<SerieEstado[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    obtenerOcupacionHistorica(60)
      .then(setSeries)
      .catch((err: Error) => setError(err.message))
      .finally(() => setCargando(false))
  }, [])

  if (cargando) {
    return (
      <div className="monitoreo-panel">
        <p className="monitoreo-panel__status-text">Cargando ocupacion historica...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="monitoreo-panel">
        <p className="monitoreo-panel__status-text monitoreo-panel__status-text--error">
          No se pudo conectar con el servicio de metricas: {error}
        </p>
      </div>
    )
  }

  const datosGrafico = aFormatoGrafico(series)

  return (
    <div className="monitoreo-panel">
      <header className="monitoreo-panel__header">
        <h2 className="monitoreo-panel__title">Ocupacion en la ultima hora</h2>
      </header>

      {datosGrafico.length === 0 ? (
        <p className="monitoreo-panel__status-text">Aun no hay historico suficiente para graficar.</p>
      ) : (
        <ResponsiveContainer width="100%" height={320}>
          <LineChart data={datosGrafico} margin={{ top: 10, right: 20, left: 0, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#dbe5dd" />
            <XAxis dataKey="instante" tickFormatter={formatearHora} tick={{ fontSize: 11 }} />
            <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
            <Tooltip labelFormatter={formatearHora} />
            <Legend formatter={(estado: string) => ESTADO_LABEL[estado as EstadoLaboratorio] ?? estado} />
            {series.map((serie) => (
              <Line
                key={serie.estado}
                type="monotone"
                dataKey={serie.estado}
                name={serie.estado}
                stroke={ESTADO_COLOR[serie.estado]}
                strokeWidth={2}
                dot={false}
                isAnimationActive={false}
              />
            ))}
          </LineChart>
        </ResponsiveContainer>
      )}
    </div>
  )
}