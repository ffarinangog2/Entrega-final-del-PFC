import type { NotificacionInterna } from '../../services/operationalApi'

export function destinoNotificacion(item:NotificacionInterna){
  const ref=item.referenciaId?encodeURIComponent(item.referenciaId):''
  if(['PLANIFICACION','PLANIFICACION_APROBADA','PLANIFICACION_DEVUELTA','SOLICITUD_RETIRO','SOLICITUD_RETIRO_RESUELTA'].includes(item.tipo??'')) return `/planificacion${ref?`?planificacionId=${ref}`:''}`
  if(['USO_LABORATORIO_ABIERTO','ASISTENCIA_ABIERTA'].includes(item.tipo??'')) return `/asistencia${ref?`?sesionId=${ref}`:''}`
  if(['HORARIO_DOCENTE','CAMBIO_HORARIO'].includes(item.tipo??'')) return '/asistencia#horario'
  return '/notificaciones'
}
