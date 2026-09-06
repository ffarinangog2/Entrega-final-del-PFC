import type { Laboratorio } from '../../services/academicoApi'

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
