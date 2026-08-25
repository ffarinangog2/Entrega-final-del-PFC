import { ApiError, apiRequest } from '../../services/apiClient'

export type EstadoReserva = 'PROGRAMADA' | 'EN_CURSO' | 'FINALIZADA' | 'CANCELADA' | 'NO_ASISTIDA'
export type EstadoSolicitud = 'PENDIENTE' | 'EN_REVISION' | 'PROPUESTA' | 'APROBADA' | 'RECHAZADA' | 'CANCELADA' | 'EXPIRADA'
export interface Reserva { id: string; solicitudId: string; laboratorioId: string; responsableId: string; fechaReserva: string; horaInicio: string; horaFin: string; estado: EstadoReserva; codigoReserva: string; creadaEn: string; actualizadaEn: string; version: number }
export interface CrearSolicitudReserva { solicitanteId: string; docenteId: string; laboratorioId: string; materiaId: string; periodoLectivoId: string; fechaReserva: string; horaInicio: string; horaFin: string; numeroParticipantes: number; motivo: string; observacion: string }
export interface SolicitudReserva extends CrearSolicitudReserva { id: string; estado: EstadoSolicitud; propuestaFecha: string | null; propuestaHoraInicio: string | null; propuestaHoraFin: string | null; propuestaLaboratorioId: string | null; propuestaObservacion: string | null; reservaId: string | null; creadaEn: string; actualizadaEn: string; version: number }
export interface HistorialSolicitud { id: string; solicitudId: string; estadoAnterior: EstadoSolicitud | null; estadoNuevo: EstadoSolicitud; usuarioAccionId: string; comentario: string | null; fechaHora: string }
export interface Disponibilidad { laboratorioId: string; fecha: string; horaInicio: string; horaFin: string; disponible: boolean; motivo: string | null }
interface Pagina<T> { contenido: T[]; pagina: number; tamanio: number; totalElementos: number; totalPaginas: number; primera: boolean; ultima: boolean }
export { ApiError as ReservasApiError }

export async function obtenerReservas(): Promise<Reserva[]> { return (await apiRequest<Pagina<Reserva>>('/api/v1/reservas?tamanio=100')).contenido }
export async function obtenerSolicitudes(): Promise<SolicitudReserva[]> { return (await apiRequest<Pagina<SolicitudReserva>>('/api/v1/solicitudes?tamanio=100')).contenido }
export function obtenerReservaPorId(id: string) { return apiRequest<Reserva>(`/api/v1/reservas/${encodeURIComponent(id)}`) }
export function obtenerSolicitudPorId(id: string) { return apiRequest<SolicitudReserva>(`/api/v1/solicitudes/${encodeURIComponent(id)}`) }
export async function obtenerHistorialSolicitud(id: string) { return (await apiRequest<Pagina<HistorialSolicitud>>(`/api/v1/solicitudes/${encodeURIComponent(id)}/historial?tamanio=100`)).contenido }
export function cancelarReserva(id: string, motivo: string) { return apiRequest<Reserva>(`/api/v1/reservas/${encodeURIComponent(id)}/cancelar`, { method: 'POST', body: JSON.stringify({ motivo }) }) }
export function crearSolicitud(datos: CrearSolicitudReserva, idempotencyKey: string) { return apiRequest<SolicitudReserva>('/api/v1/solicitudes', { method: 'POST', headers: { 'Idempotency-Key': idempotencyKey }, body: JSON.stringify(datos) }) }
export function consultarDisponibilidad(laboratorioId: string, fecha: string, horaInicio: string, horaFin: string) { const query = new URLSearchParams({ fecha, horaInicio, horaFin }); return apiRequest<Disponibilidad>(`/api/v1/disponibilidad/laboratorios/${encodeURIComponent(laboratorioId)}?${query}`) }
export async function obtenerCalendario(fechaDesde: string, fechaHasta: string): Promise<Reserva[]> { const query = new URLSearchParams({ fechaDesde, fechaHasta, tamanio: '100' }); return (await apiRequest<Pagina<Reserva>>(`/api/v1/reservas/calendario?${query}`)).contenido }
export function ponerEnRevision(id: string) { return apiRequest<SolicitudReserva>(`/api/v1/solicitudes/${encodeURIComponent(id)}/revision`, { method: 'POST' }) }
export function aprobarSolicitud(id: string, responsableId: string, comentario: string, key: string) { return apiRequest<Reserva>(`/api/v1/solicitudes/${encodeURIComponent(id)}/aprobar`, { method: 'POST', headers: { 'Idempotency-Key': key }, body: JSON.stringify({ responsableId, comentario }) }) }
export function rechazarSolicitud(id: string, comentario: string) { return apiRequest<SolicitudReserva>(`/api/v1/solicitudes/${encodeURIComponent(id)}/rechazar`, { method: 'POST', body: JSON.stringify({ comentario }) }) }
export function cancelarSolicitud(id: string, comentario: string) { return apiRequest<SolicitudReserva>(`/api/v1/solicitudes/${encodeURIComponent(id)}/cancelar`, { method: 'POST', body: JSON.stringify({ comentario }) }) }
export function proponerAlternativa(id: string, propuesta: { fecha: string; horaInicio: string; horaFin: string; laboratorioId: string; observacion: string }) { return apiRequest<SolicitudReserva>(`/api/v1/solicitudes/${encodeURIComponent(id)}/propuesta`, { method: 'POST', body: JSON.stringify(propuesta) }) }
export function responderPropuesta(id: string, aceptar: boolean, comentario: string) { const accion = aceptar ? 'aceptar' : 'rechazar'; return apiRequest<SolicitudReserva>(`/api/v1/solicitudes/${encodeURIComponent(id)}/propuesta/${accion}`, { method: 'POST', body: JSON.stringify({ comentario }) }) }
