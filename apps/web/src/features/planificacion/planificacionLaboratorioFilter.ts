import type { Laboratorio } from '../../services/academicoApi'

export function formatPisoLabel(piso?: { numero: number } | null): string {
  if (!piso || typeof piso.numero !== 'number') return 'Piso'
  if (piso.numero === 0) return 'Piso 1 · Planta Baja'
  return `Piso ${piso.numero + 1}`
}

export const laboratoriosDelPiso = (
  laboratorios: Laboratorio[],
  pisoId: string,
) => pisoId
  ? laboratorios.filter((laboratorio) => laboratorio.pisoId === pisoId)
  : laboratorios

export const pisoDelLaboratorio = (
  laboratorios: Laboratorio[],
  laboratorioId: string,
) => laboratorios.find((laboratorio) => laboratorio.id === laboratorioId)?.pisoId ?? ''
