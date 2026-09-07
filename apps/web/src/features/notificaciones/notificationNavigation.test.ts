import { describe, expect, it } from 'vitest'
import { resolverDestinoNotificacion } from './notificationNavigation'
import type { NotificacionInterna } from '../../services/operationalApi'
import type { AuthUser } from '../../types/auth'

function notif(tipo: string | null, referenciaId: string | null = null): NotificacionInterna {
  return {
    id: 'n-1',
    titulo: 'Título',
    cuerpo: 'Cuerpo',
    tipo,
    referenciaId,
    leida: false,
    creadaEn: new Date().toISOString(),
  }
}

function user(rol: string): AuthUser {
  return {
    id: 'u-1',
    perfilId: 'p-1',
    username: 'user1',
    nombres: 'User',
    apellidos: 'Test',
    emailInstitucional: 'user@epn.edu.ec',
    roles: [rol],
    permisos: [],
    tiposPerfil: [],
  }
}

describe('resolverDestinoNotificacion', () => {
  it('11. resuelve SOLICITUD con referenciaId hacia /solicitudes/:id', () => {
    const destino = resolverDestinoNotificacion(notif('SOLICITUD', 'sol-123'), user('DOCENTE'))
    expect(destino).toBe('/solicitudes/sol-123')
  })

  it('11. resuelve RESERVA con referenciaId hacia /reservas/:id', () => {
    const destino = resolverDestinoNotificacion(notif('RESERVA', 'res-456'), user('DOCENTE'))
    expect(destino).toBe('/reservas/res-456')
  })

  it('11. resuelve tipos de planificacion hacia /planificacion', () => {
    expect(resolverDestinoNotificacion(notif('PLANIFICACION', 'p-1'), user('COORDINADOR'))).toBe('/planificacion')
    expect(resolverDestinoNotificacion(notif('PLANIFICACION_APROBADA', 'p-1'), user('COORDINADOR'))).toBe('/planificacion')
    expect(resolverDestinoNotificacion(notif('PLANIFICACION_DEVUELTA', 'p-1'), user('COORDINADOR'))).toBe('/planificacion')
  })

  it('13. solicitud de cambio entre pisos usa referencia y navega a /planificacion', () => {
    // Para solicitud de cambio, referenciaId puede ser el UUID de la solicitud, no el del plan.
    // La resolución debe llevar a /planificacion sin rutas inventadas.
    const destinoCoordinador = resolverDestinoNotificacion(notif('SOLICITUD_CAMBIO', 'solicitud-uuid-99'), user('COORDINADOR'))
    expect(destinoCoordinador).toBe('/planificacion')

    const destinoAdminPiso = resolverDestinoNotificacion(notif('SOLICITUD_CAMBIO', 'solicitud-uuid-99'), user('ADMINISTRADOR_PISO'))
    expect(destinoAdminPiso).toBe('/planificacion')

    const destinoResuelta = resolverDestinoNotificacion(notif('SOLICITUD_CAMBIO_RESUELTA', 'solicitud-uuid-99'), user('COORDINADOR'))
    expect(destinoResuelta).toBe('/planificacion')
  })

  it('11. resuelve solicitud de retiro hacia /planificacion', () => {
    expect(resolverDestinoNotificacion(notif('SOLICITUD_RETIRO', 'ret-1'), user('ADMINISTRADOR_PISO'))).toBe('/planificacion')
    expect(resolverDestinoNotificacion(notif('SOLICITUD_RETIRO_RESUELTA', 'ret-1'), user('COORDINADOR'))).toBe('/planificacion')
  })

  it('11. resuelve CAMBIO_HORARIO segun el rol del usuario', () => {
    expect(resolverDestinoNotificacion(notif('CAMBIO_HORARIO', 'plan-1'), user('ESTUDIANTE'))).toBe('/mi-horario')
    expect(resolverDestinoNotificacion(notif('CAMBIO_HORARIO', 'plan-1'), user('DOCENTE'))).toBe('/main')
  })

  it('11. resuelve HORARIO_DOCENTE hacia /main para docente', () => {
    expect(resolverDestinoNotificacion(notif('HORARIO_DOCENTE', 'plan-1'), user('DOCENTE'))).toBe('/main')
  })

  it('11. resuelve ASISTENCIA_ABIERTA hacia /asistencia', () => {
    expect(resolverDestinoNotificacion(notif('ASISTENCIA_ABIERTA', 'ses-1'), user('ESTUDIANTE'))).toBe('/asistencia')
    expect(resolverDestinoNotificacion(notif('ASISTENCIA', 'ses-1'), user('DOCENTE'))).toBe('/asistencia')
  })

  it('11. resuelve INCIDENTE hacia /incidentes', () => {
    expect(resolverDestinoNotificacion(notif('INCIDENTE', 'inc-1'), user('DOCENTE'))).toBe('/incidentes')
    expect(resolverDestinoNotificacion(notif('INCIDENCIA', 'inc-1'), user('ADMINISTRADOR_PISO'))).toBe('/incidentes')
  })

  it('12. tipo no resoluble o desconocido navega a fallback /notificaciones', () => {
    expect(resolverDestinoNotificacion(notif('TIPO_DESCONOCIDO', 'xyz'))).toBe('/notificaciones')
    expect(resolverDestinoNotificacion(notif(null, 'xyz'))).toBe('/notificaciones')
  })

  it('12. SOLICITUD o RESERVA sin referenciaId navega a fallback /notificaciones', () => {
    expect(resolverDestinoNotificacion(notif('SOLICITUD', null), user('DOCENTE'))).toBe('/notificaciones')
    expect(resolverDestinoNotificacion(notif('RESERVA', null), user('DOCENTE'))).toBe('/notificaciones')
  })

  it('12. usuario sin permisos o rol incompatible para la ruta navega a /notificaciones', () => {
    expect(resolverDestinoNotificacion(notif('PLANIFICACION', 'p-1'), user('ESTUDIANTE'))).toBe('/notificaciones')
    expect(resolverDestinoNotificacion(notif('SOLICITUD', 'sol-1'), user('ESTUDIANTE'))).toBe('/notificaciones')
  })
})
