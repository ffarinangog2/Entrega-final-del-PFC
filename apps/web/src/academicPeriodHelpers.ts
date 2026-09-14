import type { PeriodoLectivo } from './services/academicoApi'

export function etiquetaPeriodo(periodo: PeriodoLectivo) {
  const nombrePpa = periodo.ppaNombre?.replace(/^REGULAR\s*-\s*/i, 'REGULAR ')
  if (periodo.cicloAcademico === 1) return nombrePpa?.replace(/\s*PPA\s*$/i, '').trim() + ' PPA'
  if (periodo.cicloAcademico === 2) return nombrePpa?.replace(/\s*PPA\s*$/i, '').trim() + ' SPA'
  return periodo.nombre
}

export function estadoEfectivo(periodo: PeriodoLectivo, vigenteId?: string) {
  if (periodo.id === vigenteId) return 'ACTUAL'
  const hoy = new Date().toISOString().slice(0, 10)
  return periodo.fechaInicio > hoy ? 'PLANIFICADO' : 'FINALIZADO'
}

/**
 * Determina si un período pertenece al flujo REGULAR PPA/SPA
 * basándose en el campo estructurado `cicloAcademico` (1 = PPA, 2 = SPA).
 * Períodos ajenos como 'Periodo Lectivo 2026-A' o '2026-B' no tienen cicloAcademico.
 */
export function esPeriodoRegular(periodo: PeriodoLectivo): boolean {
  return periodo.cicloAcademico === 1 || periodo.cicloAcademico === 2
}

/**
 * Determina si un período académico está disponible para una fecha dada (YYYY-MM-DD).
 * Regla:
 * 1. Debe ser un período REGULAR (PPA o SPA identificado por cicloAcademico).
 * 2. fechaInicio <= fechaReferencia <= fechaFin.
 * Se excluyen períodos futuros cuya fecha de inicio aún no llega y períodos finalizados.
 */
export function estaPeriodoDisponible(
  periodo: PeriodoLectivo,
  fechaReferencia: string = new Date().toISOString().slice(0, 10),
): boolean {
  if (!esPeriodoRegular(periodo)) {
    return false
  }
  if (!periodo.fechaInicio || !periodo.fechaFin) {
    return false
  }
  const inicio = periodo.fechaInicio.slice(0, 10)
  const fin = periodo.fechaFin.slice(0, 10)
  return inicio <= fechaReferencia && fechaReferencia <= fin
}

/**
 * Filtra los períodos para retornar exclusivamente los períodos REGULARES disponibles en la fecha dada.
 */
export function filtrarPeriodosDisponibles(
  periodos: PeriodoLectivo[],
  fechaReferencia: string = new Date().toISOString().slice(0, 10),
): PeriodoLectivo[] {
  return periodos.filter((p) => estaPeriodoDisponible(p, fechaReferencia))
}
