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
export interface PlanificacionAgregada { id: string; carreraId: string; periodoId: string; estado: EstadoPlanificacionAgregada; bloques: Planificacion[]; revisiones: { id: string; pisoId: string; estado: string; observacion: string | null; ronda?: number; vigente?: boolean; revisadaPorPerfilId?: string | null; actualizadaEn?: string }[] }
export const listarPlanificacionesAgregadas = () => apiRequest<PlanificacionAgregada[]>('/api/v1/planificaciones-agregadas')
export const iniciarPlanificacion = (periodoId: string) => apiRequest<PlanificacionAgregada>('/api/v1/planificaciones-agregadas', { method: 'POST', body: JSON.stringify({ periodoId }) })
export const enviarPlanificacionCompleta = (id: string) => apiRequest<PlanificacionAgregada>(`/api/v1/planificaciones-agregadas/${encodeURIComponent(id)}/enviar`, { method: 'POST' })
export const retirarPlanificacionCompleta = (id: string) => apiRequest<PlanificacionAgregada>(`/api/v1/planificaciones-agregadas/${encodeURIComponent(id)}/retirar`, { method: 'POST' })
export interface DisponibilidadPlanificacion { docentesOcupados: string[]; laboratoriosOcupados: string[] }
export const obtenerDisponibilidadPlanificacion = (params: { planificacionId?: string; periodoId: string; dia: string; horaInicio: string; horaFin: string }) => {
  const query = new URLSearchParams(Object.entries(params).filter((entry): entry is [string, string] => Boolean(entry[1])))
  return apiRequest<DisponibilidadPlanificacion>(`/api/v1/planificaciones-agregadas/disponibilidad?${query}`)
}
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
  reservaId: string | null
  bloqueId?: string | null
  fechaClase?: string | null
  abiertaEn: string
  expiraEn: string
  estado: string
  token: string | null
}
export interface RegistroAsistencia {
  id: string
  sesionId: string
  estudianteId: string
  bloqueId?: string | null
  registradaEn: string
  estado: string
}
export const abrirAsistencia = (reservaId: string) =>
  apiRequest<SesionAsistencia>('/api/v1/asistencias/sesiones', {
    method: 'POST',
    body: JSON.stringify({ reservaId }),
  })
export const abrirAsistenciaBloque = (bloqueId: string) =>
  apiRequest<SesionAsistencia>('/api/v1/asistencias/sesiones', {
    method: 'POST', body: JSON.stringify({ bloqueId }),
  })
export const obtenerClasesDocenteHoy = () => apiRequest<Planificacion[]>('/api/v1/asistencias/mis-clases-hoy')
export const obtenerMiHorario = (periodoId?: string) =>
  apiRequest<Planificacion[]>(`/api/v1/asistencias/mi-horario${periodoId ? `?periodoId=${encodeURIComponent(periodoId)}` : ''}`)
export const obtenerMiHorarioDocente = (periodoId?: string) =>
  apiRequest<Planificacion[]>(`/api/v1/asistencias/mi-horario-docente${periodoId ? `?periodoId=${encodeURIComponent(periodoId)}` : ''}`)
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
export const historialAsistencia = (periodoId?: string) =>
  apiRequest<RegistroAsistencia[]>(`/api/v1/asistencias/historial${periodoId ? `?periodoId=${encodeURIComponent(periodoId)}` : ''}`)
export const listarSesionesAbiertas = () =>
  apiRequest<SesionAsistencia[]>('/api/v1/asistencias/sesiones/abiertas')
export const registrarAsistenciaPropia = (id: string) =>
  apiRequest<RegistroAsistencia>(
    `/api/v1/asistencias/sesiones/${encodeURIComponent(id)}/registro-propio`,
    { method: 'POST' },
  )
export interface NotificacionInterna { id: string; titulo: string; cuerpo: string; tipo: string | null; referenciaId: string | null; leida: boolean; creadaEn: string }
export const listarNotificaciones = () => apiRequest<NotificacionInterna[]>('/api/v1/notificaciones')
export const marcarNotificacionLeida = (id: string) => apiRequest<NotificacionInterna>(`/api/v1/notificaciones/${encodeURIComponent(id)}/leer`, { method: 'POST' })
export const marcarTodasNotificacionesLeidas = () => apiRequest<void>('/api/v1/notificaciones/leer-todas', { method: 'POST' })
export interface SolicitudCambio { id:string; planificacionId:string; bloqueId:string; tipo:'LABORATORIO'|'HORARIO'|'DOCENTE'|'CANCELACION'; estado:'PENDIENTE'|'APROBADA'|'RECHAZADA'; motivo:string; laboratorioAnteriorId:string; laboratorioPropuestoId:string; docenteAnteriorId:string|null; docentePropuestoId:string|null; diaAnterior:string; diaPropuesto:string; horaInicioAnterior:string; horaInicioPropuesta:string; horaFinAnterior:string; horaFinPropuesta:string; creadaEn:string }
export const listarSolicitudesCambio=(planId:string)=>apiRequest<SolicitudCambio[]>(`/api/v1/planificaciones-agregadas/${encodeURIComponent(planId)}/solicitudes-cambio`)
export const crearSolicitudCambio=(planId:string,body:{bloqueId:string;tipo:SolicitudCambio['tipo'];motivo:string;laboratorioId?:string;docenteId?:string;diaSemana?:string;horaInicio?:string;horaFin?:string})=>apiRequest<SolicitudCambio>(`/api/v1/planificaciones-agregadas/${encodeURIComponent(planId)}/solicitudes-cambio`,{method:'POST',body:JSON.stringify(body)})
export const aprobarSolicitudCambio=(planId:string,id:string,observacion='')=>apiRequest<SolicitudCambio>(`/api/v1/planificaciones-agregadas/${encodeURIComponent(planId)}/solicitudes-cambio/${encodeURIComponent(id)}/aprobar`,{method:'POST',body:JSON.stringify({observacion})})
export const rechazarSolicitudCambio=(planId:string,id:string,observacion:string)=>apiRequest<SolicitudCambio>(`/api/v1/planificaciones-agregadas/${encodeURIComponent(planId)}/solicitudes-cambio/${encodeURIComponent(id)}/rechazar`,{method:'POST',body:JSON.stringify({observacion})})

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
