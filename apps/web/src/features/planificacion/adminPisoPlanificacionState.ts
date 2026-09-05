import type { Planificacion } from '../../services/operationalApi'

export const estadoPaquete = (items: Planificacion[]) =>
  items.some((item) => item.estado === 'PROPUESTA_CAMBIO')
    ? 'Devuelta con observaciones'
    : items.some((item) => item.estado === 'ENVIADA')
      ? 'Pendiente de revisión'
      : items.length > 0 && items.every((item) => item.estado === 'CONFIRMADA')
        ? 'Aprobada'
        : items.some((item) => item.estado === 'RECHAZADA')
          ? 'Rechazada'
          : 'Sin envío pendiente'
