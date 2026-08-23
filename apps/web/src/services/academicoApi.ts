const GATEWAY_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export type EstadoLaboratorio = 'DISPONIBLE' | 'OCUPADO' | 'MANTENIMIENTO' | 'INACTIVO'

export interface Laboratorio {
  id: string
  pisoId: string
  codigo: string
  nombre: string
  capacidad: number
  descripcion: string
  estado: EstadoLaboratorio
  activo: boolean
  creadoEn: string
  actualizadoEn: string
}

interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
}

export async function obtenerLaboratorios(): Promise<Laboratorio[]> {
  const response = await fetch(`${GATEWAY_BASE_URL}/api/v1/laboratorios`)

  if (!response.ok) {
    throw new Error(`Error al obtener laboratorios: ${response.status}`)
  }

  const data: PageResponse<Laboratorio> = await response.json()
  return data.content
}

export async function obtenerLaboratoriosDisponibles(): Promise<Laboratorio[]> {
  const response = await fetch(`${GATEWAY_BASE_URL}/api/v1/laboratorios/disponibles`)

  if (!response.ok) {
    throw new Error(`Error al obtener laboratorios disponibles: ${response.status}`)
  }

  return response.json()

}

export interface PuntoSerie {
  instante: string
  valor: number
}

export interface SerieEstado {
  estado: EstadoLaboratorio
  puntos: PuntoSerie[]
}

export async function obtenerOcupacionHistorica(rangoMinutos = 60): Promise<SerieEstado[]> {
  const response = await fetch(
    `${GATEWAY_BASE_URL}/api/v1/laboratorios/metricas/ocupacion?rangoMinutos=${rangoMinutos}`
  )

  if (!response.ok) {
    throw new Error(`Error al obtener ocupacion historica: ${response.status}`)
  }

  return response.json()
}