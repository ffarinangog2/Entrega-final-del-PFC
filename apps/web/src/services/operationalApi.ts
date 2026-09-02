import { apiRequest } from './apiClient'

export type EstadoPlanificacion =
  | 'BORRADOR'
  | 'ENVIADA'
  | 'PROPUESTA_CAMBIO'
  | 'CONFIRMADA'
  | 'RECHAZADA'
  | 'CANCELADA'
export interface Planificacion {
  id: string
  planificacionId?: string | null
  nivel?: number | null
  periodoId: string
  carreraId: string
  materiaId: string
  docenteId: string | null
  laboratorioId: string
  diaSemana: string
  horaInicio: string
  horaFin: string
  estado: EstadoPlanificacion
  observacion: string | null
  version: number
}
export interface GuardarPlanificacion {
  planificacionId?: string
  nivel?: number
  periodoId: string
  carreraId: string
  materiaId: string
  docenteId: string | null
  laboratorioId: string
  diaSemana: string
  horaInicio: string
  horaFin: string
  observacion: string
}
export type EstadoPlanificacionAgregada = 'BORRADOR' | 'EN_REVISION' | 'REQUIERE_CAMBIOS' | 'APROBADA' | 'FINALIZADA'
export interface PlanificacionAgregada { id: string; carreraId: string; periodoId: string; estado: EstadoPlanificacionAgregada; bloques: Planificacion[]; revisiones: { id: string; pisoId: string; estado: string; observacion: string | null }[] }
export const listarPlanificacionesAgregadas = () => apiRequest<PlanificacionAgregada[]>('/api/v1/planificaciones-agregadas')
export const iniciarPlanificacion = (periodoId: string) => apiRequest<PlanificacionAgregada>('/api/v1/planificaciones-agregadas', { method: 'POST', body: JSON.stringify({ periodoId }) })
export const enviarPlanificacionCompleta = (id: string) => apiRequest<PlanificacionAgregada>(`/api/v1/planificaciones-agregadas/${encodeURIComponent(id)}/enviar`, { method: 'POST' })
export const retirarPlanificacionCompleta = (id: string) => apiRequest<PlanificacionAgregada>(`/api/v1/planificaciones-agregadas/${encodeURIComponent(id)}/retirar`, { method: 'POST' })
export const aprobarPlanificacionPiso = (id: string) => apiRequest<PlanificacionAgregada>(`/api/v1/planificaciones-agregadas/${encodeURIComponent(id)}/revisiones/mi-piso/aprobar`, { method: 'POST' })
export const rechazarPlanificacionPiso = (id: string, observacion: string) => apiRequest<PlanificacionAgregada>(`/api/v1/planificaciones-agregadas/${encodeURIComponent(id)}/revisiones/mi-piso/rechazar`, { method: 'POST', body: JSON.stringify({ observacion }) })
export const proponerCambioPlanificacionPiso = (id: string, body: { bloqueId: string; laboratorioPropuestoId?: string; observacion: string }) => apiRequest<PlanificacionAgregada>(`/api/v1/planificaciones-agregadas/${encodeURIComponent(id)}/revisiones/mi-piso/proponer-cambio`, { method: 'POST', body: JSON.stringify(body) })
export const listarPlanificaciones = () =>
  apiRequest<Planificacion[]>('/api/v1/planificaciones')
export const crearPlanificacion = (body: GuardarPlanificacion) =>
  apiRequest<Planificacion>('/api/v1/planificaciones', {
    method: 'POST',
    body: JSON.stringify(body),
  })
export const editarPlanificacion = (id: string, body: GuardarPlanificacion) =>
  apiRequest<Planificacion>(
    `/api/v1/planificaciones/${encodeURIComponent(id)}`,
    { method: 'PATCH', body: JSON.stringify(body) },
  )
export const accionPlanificacion = (
  id: string,
  accion: 'enviar' | 'aceptar' | 'aceptar-propuesta' | 'reenviar' | 'cancelar',
  body?: unknown,
) =>
  apiRequest<Planificacion>(
    `/api/v1/planificaciones/${encodeURIComponent(id)}/${accion}`,
    { method: 'POST', body: body ? JSON.stringify(body) : undefined },
  )
export const rechazarPlanificacion = (id: string, observacion: string) =>
  apiRequest<Planificacion>(
    `/api/v1/planificaciones/${encodeURIComponent(id)}/rechazar`,
    { method: 'POST', body: JSON.stringify({ observacion }) },
  )
export const proponerPlanificacion = (
  id: string,
  body: {
    laboratorioId?: string
    horaInicio?: string
    horaFin?: string
    observacion: string
  },
) =>
  apiRequest<Planificacion>(
    `/api/v1/planificaciones/${encodeURIComponent(id)}/proponer-alternativa`,
    { method: 'POST', body: JSON.stringify(body) },
  )

export interface SesionAsistencia {
  id: string
  reservaId: string
  abiertaEn: string
  expiraEn: string
  estado: string
  token: string | null
}
export interface RegistroAsistencia {
  id: string
  sesionId: string
  estudianteId: string
  registradaEn: string
  estado: string
}
export const abrirAsistencia = (reservaId: string) =>
  apiRequest<SesionAsistencia>('/api/v1/asistencias/sesiones', {
    method: 'POST',
    body: JSON.stringify({ reservaId }),
  })
export const consultarAsistencia = (id: string) =>
  apiRequest<SesionAsistencia>(
    `/api/v1/asistencias/sesiones/${encodeURIComponent(id)}`,
  )
export const cerrarAsistencia = (id: string) =>
  apiRequest<void>(
    `/api/v1/asistencias/sesiones/${encodeURIComponent(id)}/cerrar`,
    { method: 'POST' },
  )
export const listarAsistentes = (id: string) =>
  apiRequest<RegistroAsistencia[]>(
    `/api/v1/asistencias/sesiones/${encodeURIComponent(id)}/registros`,
  )
export const registrarAsistencia = (id: string, token: string) =>
  apiRequest<RegistroAsistencia>(
    `/api/v1/asistencias/sesiones/${encodeURIComponent(id)}/registros`,
    { method: 'POST', body: JSON.stringify({ token }) },
  )
export const historialAsistencia = () =>
  apiRequest<RegistroAsistencia[]>('/api/v1/asistencias/historial')
export const listarSesionesAbiertas = () =>
  apiRequest<SesionAsistencia[]>('/api/v1/asistencias/sesiones/abiertas')
export const registrarAsistenciaPropia = (id: string) =>
  apiRequest<RegistroAsistencia>(
    `/api/v1/asistencias/sesiones/${encodeURIComponent(id)}/registro-propio`,
    { method: 'POST' },
  )

export interface Incidente {
  id: string
  laboratorioEquipo: string
  descripcion: string
  prioridad: string
  fecha: string
  estado: string
  creadoEn: string
}
interface Pagina<T> {
  contenido: T[]
}
export const listarIncidentes = async () =>
  (await apiRequest<Pagina<Incidente>>('/api/v1/incidentes?tamanio=100'))
    .contenido
export const crearIncidente = (body: {
  laboratorioEquipo: string
  descripcion: string
  prioridad: string
  fecha: string
}) =>
  apiRequest<Incidente>('/api/v1/incidentes', {
    method: 'POST',
    body: JSON.stringify(body),
  })
export const actualizarIncidente = (id: string, estado: string) =>
  apiRequest<Incidente>(`/api/v1/incidentes/${encodeURIComponent(id)}/estado`, {
    method: 'PATCH',
    body: JSON.stringify({ estado }),
  })
