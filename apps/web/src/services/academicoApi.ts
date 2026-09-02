import { apiRequest } from './apiClient'

export type EstadoLaboratorio = 'DISPONIBLE' | 'OCUPADO' | 'MANTENIMIENTO' | 'INACTIVO'
export interface Laboratorio { id: string; pisoId: string; codigo: string; nombre: string; capacidad: number; descripcion: string; estado: EstadoLaboratorio; activo: boolean; creadoEn: string; actualizadoEn: string }
export interface Docente { id: string; perfilId: string; codigoDocente: string | null; activo: boolean }
export interface Materia { id: string; carreraId: string; codigo: string; nombre: string; numeroHoras: number; nivel?: number | null; activo: boolean }
export interface PeriodoLectivo { id: string; codigo: string; nombre: string; fechaInicio: string; fechaFin: string; estado: 'PLANIFICADO' | 'ACTIVO' | 'FINALIZADO'; ppaCodigo?: string | null; ppaNombre?: string | null; cicloAcademico?: number | null }
export interface Carrera { id:string; facultadId:string; codigo:string; nombre:string; activo:boolean }
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

export interface PuntoSerie { instante: string; valor: number }
export interface SerieEstado { estado: EstadoLaboratorio; puntos: PuntoSerie[] }
export function obtenerOcupacionHistorica(rangoMinutos = 60) { return apiRequest<SerieEstado[]>(`/api/v1/laboratorios/metricas/ocupacion?rangoMinutos=${rangoMinutos}`) }
