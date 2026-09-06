import { describe, expect, it } from 'vitest'
import type { NotificacionInterna } from '../../services/operationalApi'
import { destinoNotificacion } from './notificationNavigation'

function notif(tipo?: string | null, referenciaId?: string | null): NotificacionInterna {
  return {
    id: 'n-test',
    titulo: 'Test Notificación',
    cuerpo: 'Contenido de prueba',
    tipo: tipo ?? null,
    referenciaId: referenciaId ?? null,
    leida: false,
    creadaEn: '2026-09-06T10:00:00Z',
  }
}

describe('destinoNotificacion', () => {
  it('resuelve rutas de planificación con y sin referencia', () => {
    expect(destinoNotificacion(notif('PLANIFICACION', 'plan-123'))).toBe(
      '/planificacion?planificacionId=plan-123',
    )
    expect(destinoNotificacion(notif('PLANIFICACION', null))).toBe('/planificacion')
    expect(destinoNotificacion(notif('PLANIFICACION_APROBADA', 'plan-abc'))).toBe(
      '/planificacion?planificacionId=plan-abc',
    )
    expect(destinoNotificacion(notif('PLANIFICACION_APROBADA'))).toBe('/planificacion')
    expect(destinoNotificacion(notif('PLANIFICACION_DEVUELTA', 'plan-dev'))).toBe(
      '/planificacion?planificacionId=plan-dev',
    )
    expect(destinoNotificacion(notif('SOLICITUD_RETIRO', 'ret-1'))).toBe(
      '/planificacion?planificacionId=ret-1',
    )
    expect(destinoNotificacion(notif('SOLICITUD_RETIRO_RESUELTA', 'ret-ok'))).toBe(
      '/planificacion?planificacionId=ret-ok',
    )
  })

  it('codifica correctamente caracteres especiales en referenciaId', () => {
    expect(destinoNotificacion(notif('PLANIFICACION', 'plan 1&2'))).toBe(
      '/planificacion?planificacionId=plan%201%262',
    )
  })

  it('resuelve rutas de asistencia y uso de laboratorio con y sin referencia', () => {
    expect(destinoNotificacion(notif('USO_LABORATORIO_ABIERTO', 'ses-456'))).toBe(
      '/asistencia?sesionId=ses-456',
    )
    expect(destinoNotificacion(notif('USO_LABORATORIO_ABIERTO'))).toBe('/asistencia')
    expect(destinoNotificacion(notif('ASISTENCIA_ABIERTA', 'ses-789'))).toBe(
      '/asistencia?sesionId=ses-789',
    )
    expect(destinoNotificacion(notif('ASISTENCIA_ABIERTA', null))).toBe('/asistencia')
  })

  it('resuelve ancla de horario docente y cambio de horario', () => {
    expect(destinoNotificacion(notif('HORARIO_DOCENTE'))).toBe('/asistencia#horario')
    expect(destinoNotificacion(notif('CAMBIO_HORARIO', 'ignored-ref'))).toBe(
      '/asistencia#horario',
    )
  })

  it('retorna fallback a /notificaciones para tipos desconocidos, vacíos o ausentes', () => {
    expect(destinoNotificacion(notif('OTRO_TIPO'))).toBe('/notificaciones')
    expect(destinoNotificacion(notif(undefined))).toBe('/notificaciones')
    expect(destinoNotificacion(notif(''))).toBe('/notificaciones')
  })
})
