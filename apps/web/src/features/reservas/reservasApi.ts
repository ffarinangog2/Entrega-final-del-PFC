const GATEWAY_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '')
export type EstadoReserva = 'PROGRAMADA' | 'EN_CURSO' | 'FINALIZADA' | 'CANCELADA' | 'NO_ASISTIDA'
export type EstadoSolicitud = 'PENDIENTE' | 'EN_REVISION' | 'APROBADA' | 'RECHAZADA' | 'CANCELADA' | 'EXPIRADA'
export interface Reserva { id: string; solicitudId: string; laboratorioId: string; responsableId: string; fechaReserva: string; horaInicio: string; horaFin: string; estado: EstadoReserva; codigoReserva: string; creadaEn: string; actualizadaEn: string; version: number }
export interface CrearSolicitudReserva { solicitanteId: string; docenteId: string; laboratorioId: string; materiaId: string; periodoLectivoId: string; fechaReserva: string; horaInicio: string; horaFin: string; numeroParticipantes: number; motivo: string; observacion: string }
export interface SolicitudReserva extends CrearSolicitudReserva { id: string; estado: EstadoSolicitud; reservaId: string | null; creadaEn: string; actualizadaEn: string; version: number }
export interface Disponibilidad { laboratorioId: string; fecha: string; horaInicio: string; horaFin: string; disponible: boolean; motivo: string | null }
interface PaginaReservas { contenido: Reserva[]; pagina: number; tamanio: number; totalElementos: number; totalPaginas: number; primera: boolean; ultima: boolean }
export class ReservasApiError extends Error { constructor(public readonly status: number, message: string) { super(message); this.name = 'ReservasApiError' } }
async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = sessionStorage.getItem('accessToken')
  let response: Response
  try { response = await fetch(`${GATEWAY_BASE_URL}${path}`, { ...init, headers: { ...(init.body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}), ...init.headers } }) }
  catch { throw new ReservasApiError(503, 'El servicio de reservas no está disponible.') }
  if (!response.ok) throw new ReservasApiError(response.status, `No se pudo completar la consulta (${response.status}).`)
  return (await response.json()) as T
}
export async function obtenerReservas(): Promise<Reserva[]> { return (await request<PaginaReservas>('/api/v1/reservas')).contenido }
export function obtenerReservaPorId(id: string) { return request<Reserva>(`/api/v1/reservas/${encodeURIComponent(id)}`) }
export function cancelarReserva(id: string, motivo: string) { return request<Reserva>(`/api/v1/reservas/${encodeURIComponent(id)}/cancelar`, { method: 'POST', body: JSON.stringify({ motivo }) }) }
export function crearSolicitud(datos: CrearSolicitudReserva, idempotencyKey: string) { return request<SolicitudReserva>('/api/v1/solicitudes', { method: 'POST', headers: { 'Idempotency-Key': idempotencyKey }, body: JSON.stringify(datos) }) }
export function consultarDisponibilidad(laboratorioId: string, fecha: string, horaInicio: string, horaFin: string) { const query = new URLSearchParams({ fecha, horaInicio, horaFin }); return request<Disponibilidad>(`/api/v1/disponibilidad/laboratorios/${encodeURIComponent(laboratorioId)}?${query}`) }
export async function obtenerCalendario(fechaDesde: string, fechaHasta: string): Promise<Reserva[]> { const query = new URLSearchParams({ fechaDesde, fechaHasta, tamanio: '100' }); return (await request<PaginaReservas>(`/api/v1/reservas/calendario?${query}`)).contenido }
