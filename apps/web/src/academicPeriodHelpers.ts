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
