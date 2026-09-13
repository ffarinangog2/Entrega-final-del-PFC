import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from './apiClient'
import {
  obtenerDocentePorPerfil, obtenerDocentes, obtenerHorariosDocente,
  obtenerLaboratorios, obtenerLaboratoriosDisponibles, obtenerMaterias,
  obtenerOcupacionHistorica, obtenerPeriodoActual,
} from './academicoApi'
import {
  aprobarSolicitud, cancelarReserva, cancelarSolicitud, consultarDisponibilidad,
  crearSolicitud, obtenerCalendario, obtenerHistorialSolicitud, obtenerReservaPorId,
  obtenerReservas, obtenerSolicitudPorId, obtenerSolicitudes, ponerEnRevision,
  proponerAlternativa, rechazarSolicitud, responderPropuesta,
} from '../features/reservas/reservasApi'

vi.mock('./apiClient', async (importOriginal) => {
  const original = await importOriginal<typeof import('./apiClient')>()
  return { ...original, apiRequest: vi.fn() }
})

const request = vi.mocked(apiRequest)

describe('adaptadores API de dominio', () => {
  beforeEach(() => request.mockReset())

  it('construye las consultas académicas y extrae páginas', async () => {
    request.mockResolvedValue({ content: ['dato'] })
    await expect(obtenerLaboratorios()).resolves.toEqual(['dato'])
    await obtenerLaboratoriosDisponibles()
    await obtenerDocentePorPerfil('perfil /1')
    await expect(obtenerDocentes()).resolves.toEqual(['dato'])
    await obtenerHorariosDocente('docente /1')
    await expect(obtenerMaterias()).resolves.toEqual(['dato'])
    await obtenerPeriodoActual()
    await obtenerOcupacionHistorica()
    await obtenerOcupacionHistorica(15)

    expect(request.mock.calls.map(([url]) => url)).toEqual([
      '/api/v1/laboratorios?size=100',
      '/api/v1/laboratorios/disponibles',
      '/api/v1/docentes/perfil/perfil%20%2F1',
      '/api/v1/docentes?size=100',
      '/api/v1/horarios/docente/docente%20%2F1',
      '/api/v1/materias?size=100',
      '/api/v1/periodos-lectivos/actual',
      '/api/v1/laboratorios/metricas/ocupacion?rangoMinutos=60',
      '/api/v1/laboratorios/metricas/ocupacion?rangoMinutos=15',
    ])
  })

  it('construye consultas y extrae páginas de reservas', async () => {
    request.mockResolvedValue({ contenido: ['dato'] })
    await expect(obtenerReservas()).resolves.toEqual(['dato'])
    await expect(obtenerSolicitudes()).resolves.toEqual(['dato'])
    await obtenerReservaPorId('reserva /1')
    await obtenerSolicitudPorId('solicitud /1')
    await expect(obtenerHistorialSolicitud('solicitud /1')).resolves.toEqual(['dato'])
    await obtenerCalendario('2026-01-01', '2026-01-31')

    expect(request.mock.calls.map(([url]) => url)).toEqual([
      '/api/v1/reservas?tamanio=100',
      '/api/v1/solicitudes?tamanio=100',
      '/api/v1/reservas/reserva%20%2F1',
      '/api/v1/solicitudes/solicitud%20%2F1',
      '/api/v1/solicitudes/solicitud%20%2F1/historial?tamanio=100',
      '/api/v1/reservas/calendario?fechaDesde=2026-01-01&fechaHasta=2026-01-31&tamanio=100',
    ])
  })

  it('envía comandos de reservas con método, headers y payload correctos', async () => {
    request.mockResolvedValue({})
    const solicitud = {
      solicitanteId: 's', docenteId: 'd', laboratorioId: 'l', materiaId: 'm',
      periodoLectivoId: 'p', fechaReserva: '2026-01-01', horaInicio: '08:00',
      horaFin: '10:00', numeroParticipantes: 10, motivo: 'Clase', observacion: '',
    }
    await cancelarReserva('r 1', 'motivo')
    await crearSolicitud(solicitud, 'key')
    await consultarDisponibilidad('lab 1', '2026-01-01', '08:00', '10:00')
    await ponerEnRevision('s 1')
    await aprobarSolicitud('s 1', 'u', 'ok', 'key')
    await rechazarSolicitud('s 1', 'no')
    await cancelarSolicitud('s 1', 'cancelar')
    await proponerAlternativa('s 1', { fecha: '2026-01-02', horaInicio: '09:00', horaFin: '11:00', laboratorioId: 'l2', observacion: 'alt' })
    await responderPropuesta('s 1', true, 'sí')
    await responderPropuesta('s 1', false, 'no')

    expect(request).toHaveBeenCalledTimes(10)
    expect(request.mock.calls[0]).toEqual(['/api/v1/reservas/r%201/cancelar', { method: 'POST', body: JSON.stringify({ motivo: 'motivo' }) }])
    expect(request.mock.calls[1][1]).toMatchObject({ method: 'POST', headers: { 'Idempotency-Key': 'key' } })
    expect(request.mock.calls[2][0]).toContain('fecha=2026-01-01&horaInicio=08%3A00&horaFin=10%3A00')
    expect(request.mock.calls[8][0]).toContain('/propuesta/aceptar')
    expect(request.mock.calls[9][0]).toContain('/propuesta/rechazar')
  })
})
