package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.UsuariosClient;
import ec.edu.scli.reservas.client.dto.LaboratorioExternoResponse;
import ec.edu.scli.reservas.client.dto.PeriodoExternoResponse;
import ec.edu.scli.reservas.domain.model.ActorAutenticado;
import ec.edu.scli.reservas.domain.model.ContextoInstitucional;
import ec.edu.scli.reservas.domain.model.EstadoPlanificacion;
import ec.edu.scli.reservas.domain.model.EstadoPlanificacionAgregada;
import ec.edu.scli.reservas.domain.model.EstadoRevisionPlanificacion;
import ec.edu.scli.reservas.domain.port.out.ActorActualPort;
import ec.edu.scli.reservas.domain.port.out.ContextoInstitucionalPort;
import ec.edu.scli.reservas.infrastructure.persistence.entity.ObservacionRevisionPlanificacionJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.PlanificacionAgregadaJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.PlanificacionJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.RevisionPlanificacionPisoJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.repository.ObservacionRevisionPlanificacionJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.PlanificacionAgregadaJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.PlanificacionJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.ReservaSpringDataRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.RevisionPlanificacionPisoJpaRepository;
import ec.edu.scli.reservas.presentation.dto.request.ProponerCambioAgregadoRequest;
import ec.edu.scli.reservas.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanificacionAgregadaServiceTest {
    private PlanificacionAgregadaJpaRepository planes;
    private PlanificacionJpaRepository bloques;
    private RevisionPlanificacionPisoJpaRepository revisiones;
    private ActorActualPort actores;
    private ContextoInstitucionalPort contextos;
    private AcademicoLaboratoriosClient academico;
    private PoliticaAmbitoLaboratorio ambito;
    private PlanificacionAgregadaService service;
    private ObservacionRevisionPlanificacionJpaRepository observaciones;
    private UsuariosClient usuarios;
    private ReservaSpringDataRepository reservasOperativas;
    private final UUID perfil = UUID.randomUUID();
    private final UUID carrera = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        planes = mock(PlanificacionAgregadaJpaRepository.class);
        bloques = mock(PlanificacionJpaRepository.class);
        revisiones = mock(RevisionPlanificacionPisoJpaRepository.class);
        actores = mock(ActorActualPort.class);
        contextos = mock(ContextoInstitucionalPort.class);
        academico = mock(AcademicoLaboratoriosClient.class);
        ambito = mock(PoliticaAmbitoLaboratorio.class);
        observaciones = mock(ObservacionRevisionPlanificacionJpaRepository.class);
        usuarios = mock(UsuariosClient.class);
        reservasOperativas = mock(ReservaSpringDataRepository.class);
        service = new PlanificacionAgregadaService(planes, bloques, revisiones, actores, contextos,
                academico, ambito, usuarios, mock(NotificacionService.class),
                observaciones, reservasOperativas);
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_COORDINADOR")));
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(
                new ContextoInstitucional(true, true, false, false, false, null, List.of(carrera)));
        when(planes.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(revisiones.findByPlanificacionId(any())).thenReturn(List.of());
        when(observaciones.findByRevisionId(any())).thenReturn(List.of());
        when(usuarios.obtenerAdministradoresPorPiso(any())).thenReturn(List.of(UUID.randomUUID()));
    }

    @Test
    void enviaUnaPlanificacionCompletaYGeneraUnaRevisionPorPisoUnico() {
        UUID planId = UUID.randomUUID();
        UUID pisoUno = UUID.randomUUID();
        UUID pisoDos = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = plan(planId, EstadoPlanificacionAgregada.BORRADOR);
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        PlanificacionJpaEntity uno = bloque(planId, 1, UUID.randomUUID(), UUID.randomUUID());
        PlanificacionJpaEntity dos = bloque(planId, 5, UUID.randomUUID(), UUID.randomUUID());
        PlanificacionJpaEntity tres = bloque(planId, 8, UUID.randomUUID(), UUID.randomUUID());
        when(bloques.findByPlanificacionId(planId)).thenReturn(List.of(uno, dos, tres));
        when(academico.obtenerLaboratorio(uno.getLaboratorioId())).thenReturn(laboratorio(uno, pisoUno));
        when(academico.obtenerLaboratorio(dos.getLaboratorioId())).thenReturn(laboratorio(dos, pisoDos));
        when(academico.obtenerLaboratorio(tres.getLaboratorioId())).thenReturn(laboratorio(tres, pisoUno));

        service.enviar(planId);

        assertThat(plan.getEstado()).isEqualTo(EstadoPlanificacionAgregada.EN_REVISION);
        var captor = org.mockito.ArgumentCaptor.forClass(RevisionPlanificacionPisoJpaEntity.class);
        verify(revisiones, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(RevisionPlanificacionPisoJpaEntity::getPisoId)
                .containsExactlyInAnyOrder(pisoUno, pisoDos);
    }

    @Test
    void aprobacionParcialNoApruebaYTodosLosPisosSiAprueban() {
        UUID planId = UUID.randomUUID();
        UUID piso = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = plan(planId, EstadoPlanificacionAgregada.EN_REVISION);
        RevisionPlanificacionPisoJpaEntity propia = new RevisionPlanificacionPisoJpaEntity();
        propia.setId(UUID.randomUUID()); propia.setPlanificacionId(planId); propia.setPisoId(piso);
        propia.setEstado(EstadoRevisionPlanificacion.PENDIENTE);
        RevisionPlanificacionPisoJpaEntity pendiente = new RevisionPlanificacionPisoJpaEntity();
        pendiente.setId(UUID.randomUUID()); pendiente.setPlanificacionId(planId); pendiente.setPisoId(UUID.randomUUID());
        pendiente.setEstado(EstadoRevisionPlanificacion.PENDIENTE);
        when(ambito.pisoGestionado()).thenReturn(piso);
        when(revisiones.findByPlanificacionIdAndPisoIdAndVigenteTrue(planId, piso)).thenReturn(Optional.of(propia));
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(revisiones.findByPlanificacionIdAndVigenteTrue(planId)).thenReturn(List.of(propia, pendiente));
        when(revisiones.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.aprobarPiso(planId);
        assertThat(plan.getEstado()).isEqualTo(EstadoPlanificacionAgregada.EN_REVISION);

        pendiente.setEstado(EstadoRevisionPlanificacion.APROBADA);
        service.aprobarPiso(planId);
        assertThat(plan.getEstado()).isEqualTo(EstadoPlanificacionAgregada.APROBADA);
    }

    @Test
    void iniciarReutilizaLaPlanificacionExistenteDelCiclo() {
        UUID periodo = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity existente = plan(UUID.randomUUID(),
                EstadoPlanificacionAgregada.BORRADOR);
        existente.setPeriodoId(periodo);
        when(academico.existePeriodoLectivo(periodo)).thenReturn(true);
        when(planes.findByCarreraIdAndPeriodoId(carrera, periodo))
                .thenReturn(Optional.of(existente));

        var response = service.iniciar(periodo);

        assertThat(response.id()).isEqualTo(existente.getId());
        verify(planes, never()).saveAndFlush(any());
    }

    @Test
    void enviarRechazaPlanificacionSinBloques() {
        UUID planId = UUID.randomUUID();
        when(planes.findById(planId)).thenReturn(Optional.of(
                plan(planId, EstadoPlanificacionAgregada.BORRADOR)));
        when(bloques.findByPlanificacionId(planId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.enviar(planId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no contiene bloques");
        verify(revisiones, never()).save(any());
    }

    @Test
    void retirarPlanificacionEnRevisionConservaBloquesYVuelveABorrador() {
        UUID planId = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = plan(planId,
                EstadoPlanificacionAgregada.EN_REVISION);
        plan.setEnviadaEn(Instant.now());
        PlanificacionJpaEntity bloque = bloque(planId, 4, UUID.randomUUID(),
                UUID.randomUUID());
        RevisionPlanificacionPisoJpaEntity revision = new RevisionPlanificacionPisoJpaEntity();
        revision.setId(UUID.randomUUID());
        revision.setPlanificacionId(planId);
        revision.setEstado(EstadoRevisionPlanificacion.PENDIENTE);
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(revisiones.findByPlanificacionIdAndVigenteTrue(planId)).thenReturn(List.of(revision));
        when(bloques.findByPlanificacionId(planId)).thenReturn(List.of(bloque));

        var response = service.retirar(planId);

        assertThat(response.estado()).isEqualTo("BORRADOR");
        assertThat(response.bloques()).hasSize(1);
        assertThat(plan.getEnviadaEn()).isNull();
        assertThat(revision.getVigente()).isFalse();
        verify(revisiones).saveAll(List.of(revision));
    }

    @Test
    void retirarNoPermiteModificarUnaPlanificacionAprobada() {
        UUID planId = UUID.randomUUID();
        when(planes.findById(planId)).thenReturn(Optional.of(
                plan(planId, EstadoPlanificacionAgregada.APROBADA)));

        assertThatThrownBy(() -> service.retirar(planId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("en revision");
        verify(revisiones, never()).deleteAll(any());
    }

    @Test
    void enviarDetectaLaboratorioSolapadoEntreNiveles() {
        UUID planId = UUID.randomUUID();
        UUID laboratorio = UUID.randomUUID();
        PlanificacionJpaEntity primero = bloque(planId, 2, UUID.randomUUID(), laboratorio);
        PlanificacionJpaEntity segundo = bloque(planId, 7, UUID.randomUUID(), laboratorio);
        segundo.setHoraInicio(primero.getHoraInicio());
        segundo.setHoraFin(primero.getHoraFin());
        when(planes.findById(planId)).thenReturn(Optional.of(
                plan(planId, EstadoPlanificacionAgregada.BORRADOR)));
        when(bloques.findByPlanificacionId(planId)).thenReturn(List.of(primero, segundo));

        assertThatThrownBy(() -> service.enviar(planId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("laboratorio");
    }

    @Test
    void enviarDetectaDocenteSolapadoEntreNiveles() {
        UUID planId = UUID.randomUUID();
        UUID docente = UUID.randomUUID();
        PlanificacionJpaEntity primero = bloque(planId, 3, docente, UUID.randomUUID());
        PlanificacionJpaEntity segundo = bloque(planId, 8, docente, UUID.randomUUID());
        segundo.setHoraInicio(primero.getHoraInicio());
        segundo.setHoraFin(primero.getHoraFin());
        when(planes.findById(planId)).thenReturn(Optional.of(
                plan(planId, EstadoPlanificacionAgregada.BORRADOR)));
        when(bloques.findByPlanificacionId(planId)).thenReturn(List.of(primero, segundo));

        assertThatThrownBy(() -> service.enviar(planId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("docente");
    }

    @Test
    void rechazarPisoExigeUnaObservacionHumana() {
        assertThatThrownBy(() -> service.rechazarPiso(UUID.randomUUID(), " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("observacion");
        verify(revisiones, never()).saveAndFlush(any());
    }

    @Test
    void administradorGlobalListaTodasLasPlanificaciones() {
        PlanificacionAgregadaJpaEntity plan = plan(UUID.randomUUID(),
                EstadoPlanificacionAgregada.EN_REVISION);
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil,
                Set.of("ROLE_ADMINISTRADOR")));
        when(planes.findAll()).thenReturn(List.of(plan));
        when(bloques.findByPlanificacionId(plan.getId())).thenReturn(List.of());

        var resultado = service.listar();

        assertThat(resultado).singleElement()
                .extracting(response -> response.id())
                .isEqualTo(plan.getId());
    }

    @Test
    void listarExponeFinalizadaPorFechaSinPersistirCambios() {
        var plan = plan(UUID.randomUUID(), EstadoPlanificacionAgregada.APROBADA);
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_ADMINISTRADOR")));
        when(planes.findAll()).thenReturn(List.of(plan));
        when(bloques.findByPlanificacionId(plan.getId())).thenReturn(List.of());
        when(academico.obtenerPeriodo(plan.getPeriodoId())).thenReturn(new PeriodoExternoResponse(
                plan.getPeriodoId(), "P", "P", LocalDate.now().minusMonths(2), LocalDate.now().minusDays(1), "FINALIZADO", "P", "PPA", 1));
        assertThat(service.listar()).singleElement().extracting(x -> x.estado()).isEqualTo("FINALIZADA");
        assertThat(plan.getEstado()).isEqualTo(EstadoPlanificacionAgregada.APROBADA);
        verify(planes, never()).save(any());
    }

    @Test
    void administradorPisoListaSoloPlanificacionDeSuAmbito() {
        UUID piso = UUID.randomUUID();
        UUID otroPiso = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity visible = plan(UUID.randomUUID(),
                EstadoPlanificacionAgregada.EN_REVISION);
        RevisionPlanificacionPisoJpaEntity propia = new RevisionPlanificacionPisoJpaEntity();
        propia.setPlanificacionId(visible.getId());
        propia.setPisoId(piso);
        RevisionPlanificacionPisoJpaEntity ajena = new RevisionPlanificacionPisoJpaEntity();
        ajena.setPlanificacionId(UUID.randomUUID());
        ajena.setPisoId(otroPiso);
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil,
                Set.of("ROLE_ADMINISTRADOR_PISO")));
        when(ambito.pisoGestionado()).thenReturn(piso);
        when(revisiones.findAll()).thenReturn(List.of(propia, ajena));
        when(planes.findById(visible.getId())).thenReturn(Optional.of(visible));
        when(bloques.findByPlanificacionId(visible.getId())).thenReturn(List.of());

        var resultado = service.listar();

        assertThat(resultado).singleElement()
                .extracting(response -> response.id())
                .isEqualTo(visible.getId());
    }

    @Test
    void rechazoDePisoDevuelveLaPlanificacionParaCorrecciones() {
        UUID planId = UUID.randomUUID();
        UUID piso = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = plan(planId,
                EstadoPlanificacionAgregada.EN_REVISION);
        RevisionPlanificacionPisoJpaEntity revision = new RevisionPlanificacionPisoJpaEntity();
        revision.setId(UUID.randomUUID());
        revision.setPlanificacionId(planId);
        revision.setPisoId(piso);
        revision.setEstado(EstadoRevisionPlanificacion.PENDIENTE);
        when(ambito.pisoGestionado()).thenReturn(piso);
        when(revisiones.findByPlanificacionIdAndPisoIdAndVigenteTrue(planId, piso))
                .thenReturn(Optional.of(revision));
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(revisiones.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(planes.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bloques.findByPlanificacionId(planId)).thenReturn(List.of());

        var response = service.rechazarPiso(planId, "Conflicto operativo");

        assertThat(response.estado()).isEqualTo("REQUIERE_CAMBIOS");
        assertThat(revision.getEstado()).isEqualTo(EstadoRevisionPlanificacion.RECHAZADA);
        assertThat(revision.getObservacion()).isEqualTo("Conflicto operativo");
    }

    @Test
    void disponibilidadValidaFranjaYRetornaOcupacion() {
        UUID planId = UUID.randomUUID(), periodoId = UUID.randomUUID();
        assertThatThrownBy(() -> service.disponibilidad(planId, periodoId, "LUNES", null, LocalTime.of(10, 0)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("franja horaria no es valida");
        assertThatThrownBy(() -> service.disponibilidad(planId, periodoId, "LUNES", LocalTime.of(10, 0), LocalTime.of(9, 0)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("franja horaria no es valida");

        UUID doc = UUID.randomUUID(), lab = UUID.randomUUID();
        var ocupado = bloque(planId, 3, doc, lab);
        when(bloques.buscarOcupacionGlobal(eq(planId), eq(periodoId), eq("LUNES"), eq(LocalTime.of(8, 0)), eq(LocalTime.of(10, 0)), any()))
                .thenReturn(List.of(ocupado));

        var disp = service.disponibilidad(planId, periodoId, "LUNES", LocalTime.of(8, 0), LocalTime.of(10, 0));
        assertThat(disp.docentesOcupados()).containsExactly(doc);
        assertThat(disp.laboratoriosOcupados()).containsExactly(lab);
    }

    @Test
    void proponerCambioRegistraObservacionYActualizaEstado() {
        UUID planId = UUID.randomUUID(), bloqueId = UUID.randomUUID(), piso = UUID.randomUUID(), labPropuesto = UUID.randomUUID();
        RevisionPlanificacionPisoJpaEntity rev = new RevisionPlanificacionPisoJpaEntity();
        rev.setId(UUID.randomUUID());
        rev.setPlanificacionId(planId);
        rev.setPisoId(piso);
        rev.setEstado(EstadoRevisionPlanificacion.PENDIENTE);

        when(ambito.pisoGestionado()).thenReturn(piso);
        when(revisiones.findByPlanificacionIdAndPisoIdAndVigenteTrue(planId, piso)).thenReturn(Optional.of(rev));

        PlanificacionJpaEntity b = bloque(planId, 4, UUID.randomUUID(), UUID.randomUUID());
        b.setId(bloqueId);
        when(bloques.findById(bloqueId)).thenReturn(Optional.of(b));
        when(academico.obtenerLaboratorio(b.getLaboratorioId())).thenReturn(laboratorio(b, piso));

        PlanificacionAgregadaJpaEntity plan = plan(planId, EstadoPlanificacionAgregada.EN_REVISION);
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(bloques.findByPlanificacionId(planId)).thenReturn(List.of(b));

        var request = new ProponerCambioAgregadoRequest(bloqueId, labPropuesto, "Mover a lab especializado");
        var resp = service.proponerCambio(planId, request);

        assertThat(resp.estado()).isEqualTo("REQUIERE_CAMBIOS");
        assertThat(rev.getEstado()).isEqualTo(EstadoRevisionPlanificacion.PROPUESTA_CAMBIO);
        assertThat(b.getEstado()).isEqualTo(EstadoPlanificacion.PROPUESTA_CAMBIO);
        verify(ambito).validarGestion(labPropuesto);
        verify(observaciones).save(any(ObservacionRevisionPlanificacionJpaEntity.class));
    }

    @Test
    void validarOcupacionOficialDetectaConflictoConReservaOperativa() {
        UUID planId = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = plan(planId, EstadoPlanificacionAgregada.BORRADOR);
        PlanificacionJpaEntity b = bloque(planId, 4, UUID.randomUUID(), UUID.randomUUID());
        b.setDiaSemana("LUNES");
        b.setHoraInicio(LocalTime.of(8, 0));
        b.setHoraFin(LocalTime.of(10, 0));

        LocalDate hoy = LocalDate.now();
        when(academico.obtenerPeriodo(plan.getPeriodoId())).thenReturn(new PeriodoExternoResponse(
                plan.getPeriodoId(), "P", "P", hoy.minusDays(7), hoy.plusDays(7), "ACTIVO", "PPA", "PPA", 1));
        when(reservasOperativas.contarConflictosActivos(eq(b.getLaboratorioId()), any(), eq(b.getHoraInicio()), eq(b.getHoraFin())))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.validarOcupacionOficial(plan, List.of(b)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reserva operativa");
    }

    @Test
    void validarNivelInvalidoLanzaExcepcion() {
        UUID planId = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = plan(planId, EstadoPlanificacionAgregada.BORRADOR);
        PlanificacionJpaEntity b = bloque(planId, 0, UUID.randomUUID(), UUID.randomUUID()); // nivel 0 inválido

        assertThatThrownBy(() -> service.validarOcupacionOficial(plan, List.of(b)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nivel debe estar entre 1 y 10");
    }

    private PlanificacionAgregadaJpaEntity plan(UUID id, EstadoPlanificacionAgregada estado) {
        PlanificacionAgregadaJpaEntity plan = new PlanificacionAgregadaJpaEntity();
        plan.setId(id); plan.setCarreraId(carrera); plan.setPeriodoId(UUID.randomUUID());
        plan.setCoordinadorPerfilId(perfil); plan.setEstado(estado); plan.setCreadaEn(Instant.now());
        return plan;
    }

    private PlanificacionJpaEntity bloque(UUID planId, int nivel, UUID docente, UUID laboratorio) {
        PlanificacionJpaEntity bloque = new PlanificacionJpaEntity();
        bloque.setId(UUID.randomUUID()); bloque.setPlanificacionId(planId); bloque.setNivel(nivel);
        bloque.setDocenteId(docente); bloque.setLaboratorioId(laboratorio); bloque.setCarreraId(carrera);
        bloque.setPeriodoId(UUID.randomUUID()); bloque.setMateriaId(UUID.randomUUID()); bloque.setDiaSemana("MARTES");
        bloque.setHoraInicio(LocalTime.of(8 + (nivel >= 1 && nivel <= 10 ? nivel : 1), 0));
        bloque.setHoraFin(LocalTime.of(9 + (nivel >= 1 && nivel <= 10 ? nivel : 1), 0));
        return bloque;
    }

    private LaboratorioExternoResponse laboratorio(PlanificacionJpaEntity bloque, UUID piso) {
        return new LaboratorioExternoResponse(bloque.getLaboratorioId(), piso, true, true, "DISPONIBLE", 30);
    }
}
