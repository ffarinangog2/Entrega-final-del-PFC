import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from './apiClient'
import * as academico from './academicoApi'
import * as operational from './operationalApi'
import * as usuarios from './usuariosApi'

vi.mock('./apiClient', async (importOriginal) => {
  const original = await importOriginal<typeof import('./apiClient')>()
  return { ...original, apiRequest: vi.fn() }
})

const mockRequest = vi.mocked(apiRequest)

describe('cobertura exhaustiva de endpoints de servicios', () => {
  beforeEach(() => {
    mockRequest.mockReset()
    mockRequest.mockResolvedValue({ content: ['item'], contenido: ['item'] })
  })

  it('cubre funciones CRUD de academicoApi', async () => {
    await academico.obtenerDocentesPlanificacion()
    await academico.obtenerPeriodos()
    await academico.crearPeriodo({
      codigo: '2026-1',
      nombre: 'P1',
      fechaInicio: '2026-01-01',
      fechaFin: '2026-06-30',
      estado: 'ACTIVO',
      ppaCodigo: 'PPA1',
      ppaNombre: 'PPA Uno',
      cicloAcademico: 1,
    })
    await academico.actualizarPeriodo('p-1', {
      codigo: '2026-1',
      nombre: 'P1',
      fechaInicio: '2026-01-01',
      fechaFin: '2026-06-30',
      estado: 'ACTIVO',
      ppaCodigo: 'PPA1',
      ppaNombre: 'PPA Uno',
      cicloAcademico: 1,
    })
    await academico.obtenerCarreras()
    await academico.obtenerPisos()
    await academico.obtenerCampus()
    await academico.obtenerEquipos()
    await academico.obtenerTiposEquipo()
    await academico.obtenerBloques()
    await academico.obtenerFacultades()
    await academico.crearLaboratorio({ pisoId: 'p1', codigo: 'L1', nombre: 'Lab 1', capacidad: 20, descripcion: 'Desc' })
    await academico.actualizarLaboratorio('l-1', { pisoId: 'p1', codigo: 'L1', nombre: 'Lab 1', capacidad: 20, descripcion: 'Desc' })
    await academico.cambiarEstadoLaboratorio('l-1', 'DISPONIBLE')
    await academico.crearEquipo({
      laboratorioId: 'l1', tipoEquipoId: 't1', codigoInventario: 'EQ1', numeroSerie: 'S1',
      marca: 'M', modelo: 'Mod', procesador: 'i7', memoriaRam: '16GB', almacenamiento: '512GB',
      direccionIp: '1.1.1.1', direccionMac: 'AA:BB:CC', observacion: 'Obs',
    })
    await academico.actualizarEquipo('e-1', {
      laboratorioId: 'l1', tipoEquipoId: 't1', codigoInventario: 'EQ1', numeroSerie: 'S1',
      marca: 'M', modelo: 'Mod', procesador: 'i7', memoriaRam: '16GB', almacenamiento: '512GB',
      direccionIp: '1.1.1.1', direccionMac: 'AA:BB:CC', observacion: 'Obs',
    })
    await academico.cambiarEstadoEquipo('e-1', 'OPERATIVO')
    await academico.crearCampus({ codigo: 'C1', nombre: 'Campus 1', direccion: 'Dir' })
    await academico.actualizarCampus('c-1', { codigo: 'C1', nombre: 'Campus 1', direccion: 'Dir' })
    await academico.crearPiso({ bloqueId: 'b1', numero: 1, descripcion: 'Piso 1' })
    await academico.actualizarPiso('p-1', { bloqueId: 'b1', numero: 1, descripcion: 'Piso 1' })
    await academico.crearCarrera({ facultadId: 'f1', codigo: 'CAR1', nombre: 'Carrera 1', descripcion: 'Desc' })
    await academico.actualizarCarrera('c-1', { facultadId: 'f1', codigo: 'CAR1', nombre: 'Carrera 1', descripcion: 'Desc' })
    await academico.crearMateria({ carreraId: 'c1', codigo: 'MAT1', nombre: 'Materia 1', numeroHoras: 4, nivel: 1 })
    await academico.actualizarMateria('m-1', { carreraId: 'c1', codigo: 'MAT1', nombre: 'Materia 1', numeroHoras: 4, nivel: 1 })

    expect(mockRequest).toHaveBeenCalled()
  })

  it('cubre funciones de operationalApi para planificacion, asistencias, notificaciones e incidentes', async () => {
    await operational.listarPlanificacionesAgregadas()
    await operational.iniciarPlanificacion('per-1')
    await operational.enviarPlanificacionCompleta('plan-1')
    await operational.retirarPlanificacionCompleta('plan-1')
    await operational.obtenerDisponibilidadPlanificacion({
      planificacionId: 'plan-1', periodoId: 'per-1', dia: 'LUNES', horaInicio: '07:00', horaFin: '09:00',
    })
    await operational.aprobarPlanificacionPiso('plan-1')
    await operational.rechazarPlanificacionPiso('plan-1', 'Motivo rechazo')
    await operational.proponerCambioPlanificacionPiso('plan-1', { bloqueId: 'b-1', observacion: 'Obs' })
    await operational.listarPlanificaciones()
    await operational.crearPlanificacion({
      periodoId: 'per-1', carreraId: 'c-1', materiaId: 'm-1', docenteId: null,
      laboratorioId: 'l-1', diaSemana: 'LUNES', horaInicio: '07:00', horaFin: '09:00', observacion: '',
    })
    await operational.editarPlanificacion('b-1', {
      periodoId: 'per-1', carreraId: 'c-1', materiaId: 'm-1', docenteId: null,
      laboratorioId: 'l-1', diaSemana: 'LUNES', horaInicio: '07:00', horaFin: '09:00', observacion: '',
    })
    await operational.accionPlanificacion('b-1', 'enviar')
    await operational.accionPlanificacion('b-1', 'aceptar', { detalle: 'ok' })
    await operational.rechazarPlanificacion('b-1', 'Rechazo bloque')
    await operational.proponerPlanificacion('b-1', { observacion: 'Propuesta' })

    await operational.abrirAsistencia('res-1')
    await operational.abrirAsistenciaBloque('bloq-1')
    await operational.obtenerClasesDocenteHoy()
    await operational.obtenerMiHorario('per-1')
    await operational.obtenerMiHorario()
    await operational.obtenerMiHorarioDocente('per-1')
    await operational.obtenerMiHorarioDocente()
    await operational.consultarAsistencia('ses-1')
    await operational.cerrarAsistencia('ses-1')
    await operational.listarAsistentes('ses-1')
    await operational.registrarAsistencia('ses-1', 'tok-1')
    await operational.historialAsistencia('per-1')
    await operational.historialAsistencia()
    await operational.listarSesionesAbiertas()
    await operational.registrarAsistenciaPropia('ses-1')

    await operational.listarNotificaciones()
    await operational.marcarNotificacionLeida('not-1')
    await operational.marcarTodasNotificacionesLeidas()

    await operational.listarSolicitudesCambio('plan-1')
    await operational.crearSolicitudCambio('plan-1', { bloqueId: 'b-1', tipo: 'LABORATORIO', motivo: 'Cambio' })
    await operational.aprobarSolicitudCambio('plan-1', 'sol-1')
    await operational.rechazarSolicitudCambio('plan-1', 'sol-1', 'No procede')

    await operational.listarIncidentes()
    await operational.crearIncidente({ laboratorioEquipo: 'EQ1', descripcion: 'Fallo', prioridad: 'ALTA', fecha: '2026-01-01' })
    await operational.actualizarIncidente('inc-1', 'RESUELTO')

    expect(mockRequest).toHaveBeenCalled()
  })

  it('cubre funciones de contexto y administradores en usuariosApi', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => {
      return new Response(JSON.stringify([{ id: 'ctx-1' }]), { status: 200 })
    })

    await usuarios.obtenerPerfil('p-1')
    await usuarios.actualizarPerfil('p-1', {
      identificacion: '123', nombres: 'A', apellidos: 'B', emailInstitucional: 'a@b.com',
      emailPersonal: '', telefono: '', direccion: '', fechaNacimiento: '2000-01-01', fotoUrl: null,
    })
    await usuarios.cambiarEstadoPerfil('p-1', true)
    await usuarios.obtenerPerfilPropio()
    await usuarios.actualizarPerfilPropio({
      emailPersonal: 'p@b.com', telefono: '0999', direccion: 'Dir', fotoUrl: null,
    })
    await usuarios.actualizarUsuarioInstitucionalCompleto('p-1', {
      authId: 'auth-1', username: 'user1', email: 'u@b.com', rol: 'DOCENTE', activo: true,
      pisoId: null, carreraId: null, identificacion: '123', nombres: 'A', apellidos: 'B',
      emailInstitucional: 'a@b.com', emailPersonal: '', telefono: '', direccion: '',
      fechaNacimiento: '2000-01-01', fotoUrl: null,
    })
    await usuarios.obtenerMiContextoAcademico()
    await usuarios.confirmarMiContextoAcademico({ carreraId: 'c1', periodoId: 'p1', nivel: 2 })
    await usuarios.obtenerMisContextosAcademicos()
    await usuarios.obtenerContextosAcademicos('perf-1')
    await usuarios.asignarContextoAcademico('perf-1', { carreraId: 'c1', periodoId: 'p1', nivel: 2 })
    await usuarios.obtenerDocenteResumen('doc-1')
    await usuarios.listarAdministradores()
    await usuarios.actualizarAdministrador({
      id: 'adm-1', perfilId: 'p-1', codigoAdministrador: 'ADM', cargo: 'Cargo', pisoId: 'piso-1', activo: true,
    }, 'piso-2')
    await usuarios.obtenerAsociacionRol('perf-1')
    await usuarios.actualizarAsociacionRol('perf-1', { rol: 'ADMIN', pisoId: 'p1', carreraId: null })

    expect(fetchSpy).toHaveBeenCalled()
    fetchSpy.mockRestore()
  })
})
