import { apiRequest } from './apiClient'

export type EstadoLaboratorio = 'DISPONIBLE' | 'OCUPADO' | 'MANTENIMIENTO' | 'INACTIVO'
export interface Laboratorio { id: string; pisoId: string; codigo: string; nombre: string; capacidad: number; descripcion: string; estado: EstadoLaboratorio; activo: boolean; creadoEn: string; actualizadoEn: string }
export interface Docente { id: string; perfilId: string; codigoDocente: string | null; activo: boolean }
export interface Materia { id: string; carreraId: string; codigo: string; nombre: string; numeroHoras: number; nivel?: number | null; activo: boolean }
export interface PeriodoLectivo { id: string; codigo: string; nombre: string; fechaInicio: string; fechaFin: string; estado: 'PLANIFICADO' | 'ACTIVO' | 'FINALIZADO'; ppaCodigo?: string | null; ppaNombre?: string | null; cicloAcademico?: number | null }
export interface Carrera { id:string; facultadId:string; codigo:string; nombre:string; activo:boolean }
export interface Piso { id: string; bloqueId: string; numero: number; descripcion: string; activo: boolean }
export interface Campus { id: string; codigo: string; nombre: string; direccion: string; activo: boolean }
export interface Equipo { id: string; laboratorioId: string; tipoEquipoId: string; codigoInventario: string; numeroSerie: string | null; marca: string | null; modelo: string | null; estado: string; activo: boolean }
export interface TipoEquipo { id: string; nombre: string; descripcion: string; activo: boolean }
export interface Bloque { id: string; campusId: string; codigo: string; nombre: string; activo: boolean }
export interface Facultad { id: string; codigo: string; nombre: string; descripcion: string; activo: boolean }
export interface HorarioAcademico { id: string; materiaId: string; periodoLectivoId: string; laboratorioId: string | null; docenteId: string; diaSemana: string; horaInicio: string; horaFin: string; paralelo: string; activo: boolean }
interface PageResponse<T> { content: T[]; totalElements: number; totalPages: number }

export async function obtenerLaboratorios(): Promise<Laboratorio[]> { return (await apiRequest<PageResponse<Laboratorio>>('/api/v1/laboratorios?size=100')).content }
export function obtenerLaboratoriosDisponibles() { return apiRequest<Laboratorio[]>('/api/v1/laboratorios/disponibles') }
export function obtenerDocentePorPerfil(perfilId: string) { return apiRequest<Docente>(`/api/v1/docentes/perfil/${encodeURIComponent(perfilId)}`) }
export async function obtenerDocentes(): Promise<Docente[]> { return (await apiRequest<PageResponse<Docente>>('/api/v1/docentes?size=100')).content }
export function obtenerDocentesPlanificacion(): Promise<Docente[]> { return apiRequest<Docente[]>('/api/v1/docentes/planificacion') }
export function obtenerHorariosDocente(docenteId: string) { return apiRequest<HorarioAcademico[]>(`/api/v1/horarios/docente/${encodeURIComponent(docenteId)}`) }
export async function obtenerMaterias(): Promise<Materia[]> { return (await apiRequest<PageResponse<Materia>>('/api/v1/materias?size=100')).content }
export function obtenerPeriodoActual() { return apiRequest<PeriodoLectivo>('/api/v1/periodos-lectivos/actual') }
export async function obtenerPeriodos(): Promise<PeriodoLectivo[]> { return (await apiRequest<PageResponse<PeriodoLectivo>>('/api/v1/periodos-lectivos?size=100')).content }
export async function obtenerCarreras():Promise<Carrera[]>{return (await apiRequest<PageResponse<Carrera>>('/api/v1/carreras?size=100')).content}
export async function obtenerPisos(): Promise<Piso[]> { return (await apiRequest<PageResponse<Piso>>('/api/v1/pisos?size=100')).content }
export async function obtenerCampus(): Promise<Campus[]> { return (await apiRequest<PageResponse<Campus>>('/api/v1/campus?size=100')).content }
export async function obtenerEquipos(): Promise<Equipo[]> { return (await apiRequest<PageResponse<Equipo>>('/api/v1/equipos?size=100')).content }
export async function obtenerTiposEquipo(): Promise<TipoEquipo[]> { return (await apiRequest<PageResponse<TipoEquipo>>('/api/v1/tipos-equipo?size=100')).content }
export async function obtenerBloques(): Promise<Bloque[]> { return (await apiRequest<PageResponse<Bloque>>('/api/v1/bloques?size=100')).content }
export async function obtenerFacultades(): Promise<Facultad[]> { return (await apiRequest<PageResponse<Facultad>>('/api/v1/facultades?size=100')).content }
export function crearLaboratorio(body: { pisoId: string; codigo: string; nombre: string; capacidad: number; descripcion: string }) { return apiRequest<Laboratorio>('/api/v1/laboratorios', { method: 'POST', body: JSON.stringify(body) }) }
export function actualizarLaboratorio(id: string, body: { pisoId: string; codigo: string; nombre: string; capacidad: number; descripcion: string }) { return apiRequest<Laboratorio>(`/api/v1/laboratorios/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(body) }) }
export function cambiarEstadoLaboratorio(id: string, estado: EstadoLaboratorio) { return apiRequest<Laboratorio>(`/api/v1/laboratorios/${encodeURIComponent(id)}/estado`, { method: 'PATCH', body: JSON.stringify({ estado }) }) }
export function crearEquipo(body: { laboratorioId: string; tipoEquipoId: string; codigoInventario: string; numeroSerie: string; marca: string; modelo: string; procesador: string; memoriaRam: string; almacenamiento: string; direccionIp: string; direccionMac: string; observacion: string }) { return apiRequest<Equipo>('/api/v1/equipos', { method: 'POST', body: JSON.stringify(body) }) }
export function actualizarEquipo(id: string, body: { laboratorioId: string; tipoEquipoId: string; codigoInventario: string; numeroSerie: string; marca: string; modelo: string; procesador: string; memoriaRam: string; almacenamiento: string; direccionIp: string; direccionMac: string; observacion: string }) { return apiRequest<Equipo>(`/api/v1/equipos/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(body) }) }
export function cambiarEstadoEquipo(id: string, estado: string) { return apiRequest<Equipo>(`/api/v1/equipos/${encodeURIComponent(id)}/estado`, { method: 'PATCH', body: JSON.stringify({ estado }) }) }
export function crearCampus(body: { codigo: string; nombre: string; direccion: string }) { return apiRequest<Campus>('/api/v1/campus', { method: 'POST', body: JSON.stringify(body) }) }
export function actualizarCampus(id: string, body: { codigo: string; nombre: string; direccion: string }) { return apiRequest<Campus>(`/api/v1/campus/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(body) }) }
export function crearPiso(body: { bloqueId: string; numero: number; descripcion: string }) { return apiRequest<Piso>('/api/v1/pisos', { method: 'POST', body: JSON.stringify(body) }) }
export function actualizarPiso(id: string, body: { bloqueId: string; numero: number; descripcion: string }) { return apiRequest<Piso>(`/api/v1/pisos/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(body) }) }
export function crearCarrera(body: { facultadId: string; codigo: string; nombre: string; descripcion: string }) { return apiRequest<Carrera>('/api/v1/carreras', { method: 'POST', body: JSON.stringify(body) }) }
export function actualizarCarrera(id: string, body: { facultadId: string; codigo: string; nombre: string; descripcion: string }) { return apiRequest<Carrera>(`/api/v1/carreras/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(body) }) }
export function crearMateria(body: { carreraId: string; codigo: string; nombre: string; numeroHoras: number; nivel: number }) { return apiRequest<Materia>('/api/v1/materias', { method: 'POST', body: JSON.stringify(body) }) }
export function actualizarMateria(id: string, body: { carreraId: string; codigo: string; nombre: string; numeroHoras: number; nivel: number }) { return apiRequest<Materia>(`/api/v1/materias/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(body) }) }

export interface PuntoSerie { instante: string; valor: number }
export interface SerieEstado { estado: EstadoLaboratorio; puntos: PuntoSerie[] }
export function obtenerOcupacionHistorica(rangoMinutos = 60) { return apiRequest<SerieEstado[]>(`/api/v1/laboratorios/metricas/ocupacion?rangoMinutos=${rangoMinutos}`) }
