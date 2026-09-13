import type { NotificacionInterna } from '../../services/operationalApi'
import type { AuthUser } from '../../types/auth'
import { hasRole } from '../../auth'

export function resolverDestinoNotificacion(
  notificacion: NotificacionInterna,
  usuario?: AuthUser | null,
): string {
  const tipo = notificacion.tipo?.toUpperCase()
  const ref = notificacion.referenciaId

  switch (tipo) {
    case 'SOLICITUD':
      if (
        ref &&
        (!usuario ||
          hasRole(usuario, 'ADMINISTRADOR') ||
          hasRole(usuario, 'ADMINISTRADOR_PISO') ||
          hasRole(usuario, 'DOCENTE'))
      ) {
        return `/solicitudes/${encodeURIComponent(ref)}`
      }
      return '/notificaciones'

    case 'RESERVA':
      if (
        ref &&
        (!usuario ||
          hasRole(usuario, 'ADMINISTRADOR') ||
          hasRole(usuario, 'ADMINISTRADOR_PISO') ||
          hasRole(usuario, 'DOCENTE'))
      ) {
        return `/reservas/${encodeURIComponent(ref)}`
      }
      return '/notificaciones'

    case 'PLANIFICACION':
    case 'PLANIFICACION_APROBADA':
    case 'PLANIFICACION_DEVUELTA':
    case 'SOLICITUD_CAMBIO':
    case 'SOLICITUD_CAMBIO_RESUELTA':
    case 'SOLICITUD_RETIRO':
    case 'SOLICITUD_RETIRO_RESUELTA':
      if (usuario && hasRole(usuario, 'COORDINADOR')) {
        return '/planificacion'
      }
      if (usuario && hasRole(usuario, 'ADMINISTRADOR_PISO')) {
        return '/planificacion'
      }
      if (usuario && hasRole(usuario, 'ADMINISTRADOR')) {
        return '/planificacion'
      }
      if (usuario && hasRole(usuario, 'DOCENTE')) {
        return '/main'
      }
      if (!usuario) {
        return '/planificacion'
      }
      return '/notificaciones'

    case 'CAMBIO_HORARIO':
      if (usuario && hasRole(usuario, 'DOCENTE')) {
        return '/main'
      }
      if (!usuario || hasRole(usuario, 'ESTUDIANTE')) {
        return '/mi-horario'
      }
      return '/notificaciones'

    case 'HORARIO_DOCENTE':
      if (!usuario || hasRole(usuario, 'DOCENTE')) {
        return '/main'
      }
      return '/notificaciones'

    case 'ASISTENCIA_ABIERTA':
    case 'ASISTENCIA':
      if (!usuario || hasRole(usuario, 'ESTUDIANTE') || hasRole(usuario, 'DOCENTE')) {
        return '/asistencia'
      }
      return '/notificaciones'

    case 'INCIDENTE':
    case 'INCIDENCIA':
      if (
        !usuario ||
        hasRole(usuario, 'ADMINISTRADOR') ||
        hasRole(usuario, 'ADMINISTRADOR_PISO') ||
        hasRole(usuario, 'DOCENTE')
      ) {
        return '/incidentes'
      }
      return '/notificaciones'

    default:
      return '/notificaciones'
  }
}
