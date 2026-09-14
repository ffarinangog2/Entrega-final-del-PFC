package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.client.*;
import ec.edu.scli.reservas.client.dto.LaboratorioExternoResponse;
import ec.edu.scli.reservas.domain.model.*;
import ec.edu.scli.reservas.domain.port.out.*;
import ec.edu.scli.reservas.infrastructure.persistence.entity.*;
import ec.edu.scli.reservas.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SolicitudRetiroPlanificacionServiceTest {
    private SolicitudRetiroPlanificacionJpaRepository solicitudes;
    private DecisionRetiroPlanificacionJpaRepository decisiones;
    private PlanificacionAgregadaJpaRepository planes;
    private PlanificacionJpaRepository bloques;
    private RevisionPlanificacionPisoJpaRepository revisiones;
    private ActorActualPort actores;
    private ContextoInstitucionalPort contextos;
    private PoliticaAmbitoLaboratorio ambito;
    private AcademicoLaboratoriosClient academico;
    private UsuariosClient usuarios;
    private NotificacionService notificaciones;
    private SolicitudRetiroPlanificacionService service;
    private final UUID perfil = UUID.randomUUID();
    private final UUID carrera = UUID.randomUUID();
    private final Map<UUID, SolicitudRetiroPlanificacionJpaEntity> solicitudesGuardadas = new HashMap<>();
    private final List<DecisionRetiroPlanificacionJpaEntity> decisionesGuardadas = new ArrayList<>();

    @BeforeEach
    void preparar() {
        solicitudes = mock(SolicitudRetiroPlanificacionJpaRepository.class);
        decisiones = mock(DecisionRetiroPlanificacionJpaRepository.class);
        planes = mock(PlanificacionAgregadaJpaRepository.class);
        bloques = mock(PlanificacionJpaRepository.class);
        revisiones = mock(RevisionPlanificacionPisoJpaRepository.class);
        actores = mock(ActorActualPort.class);
        contextos = mock(ContextoInstitucionalPort.class);
        ambito = mock(PoliticaAmbitoLaboratorio.class);
        academico = mock(AcademicoLaboratoriosClient.class);
        usuarios = mock(UsuariosClient.class);
        notificaciones = mock(NotificacionService.class);
        service = new SolicitudRetiroPlanificacionService(solicitudes, decisiones, planes, bloques,
                revisiones, actores, contextos, ambito, academico, usuarios, notificaciones);
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_COORDINADOR")));
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(
                new ContextoInstitucional(true, true, false, false, false, null, List.of(carrera)));
        when(solicitudes.saveAndFlush(any())).thenAnswer(invocation -> guardarSolicitud(invocation.getArgument(0)));
        when(solicitudes.save(any())).thenAnswer(invocation -> guardarSolicitud(invocation.getArgument(0)));
        when(decisiones.save(any())).thenAnswer(invocation -> guardarDecision(invocation.getArgument(0)));
        when(decisiones.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(decisiones.findBySolicitudRetiroId(any())).thenAnswer(invocation -> decisionesGuardadas.stream()
                .filter(item -> invocation.getArgument(0).equals(item.getSolicitudRetiroId())).toList());
    }

    @Test
    void creaUnaDecisionPorPisoYNoDuplicaSolicitudPendiente() {
        UUID planId = UUID.randomUUID();
        UUID pisoUno = UUID.randomUUID();
        UUID pisoDos = UUID.randomUUID();
        prepararPlan(planId, pisoUno, pisoDos);

        var creada = service.crear(planId, "Corregir horarios");
        when(solicitudes.findByPlanificacionIdAndEstado(planId, EstadoSolicitudRetiro.PENDIENTE))
                .thenReturn(Optional.of(solicitudesGuardadas.get(creada.id())));
        var repetida = service.crear(planId, "Peticion repetida");

        assertThat(creada.totalPisos()).isEqualTo(2);
        assertThat(repetida.id()).isEqualTo(creada.id());
        verify(solicitudes, times(1)).saveAndFlush(any());
        verify(notificaciones, times(2)).notificarPerfilIdempotente(any(), any(), any(), any(), any());
    }

    @Test
    void exigeMotivoYRechazaPlanificacionAprobada() {
        assertThatThrownBy(() -> service.crear(UUID.randomUUID(), " "))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("motivo");
        UUID planId = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = plan(planId, EstadoPlanificacionAgregada.APROBADA);
        when(planes.findLockedById(planId)).thenReturn(Optional.of(plan));
        assertThatThrownBy(() -> service.crear(planId, "Editar"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("en revision");
    }

    @Test
    void aprobacionParcialMantieneRevisionYPlanificacion() {
        UUID planId = UUID.randomUUID();
        UUID pisoUno = UUID.randomUUID();
        UUID pisoDos = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = prepararPlan(planId, pisoUno, pisoDos);
        var creada = service.crear(planId, "Corregir horarios");
        prepararResolucion(creada.id(), plan, pisoUno);

        var resultado = service.aprobar(planId, creada.id(), "De acuerdo");

        assertThat(resultado.estado()).isEqualTo("PENDIENTE");
        assertThat(resultado.pisosAprobados()).isEqualTo(1);
        assertThat(plan.getEstado()).isEqualTo(EstadoPlanificacionAgregada.EN_REVISION);
        verify(revisiones, never()).saveAll(any());
    }

    @Test
    void aprobacionTotalRetornaABorradorYConservaHistorialDeRevision() {
        UUID planId = UUID.randomUUID();
        UUID piso = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = prepararPlan(planId, piso);
        PlanificacionJpaEntity bloque = bloques.findByPlanificacionId(planId).getFirst();
        RevisionPlanificacionPisoJpaEntity revision = new RevisionPlanificacionPisoJpaEntity();
        revision.setPlanificacionId(planId);
        revision.setPisoId(piso);
        revision.setEstado(EstadoRevisionPlanificacion.RECHAZADA);
        revision.setObservacion("Observacion historica");
        revision.setRevisadaPorPerfilId(UUID.randomUUID());
        revision.setVigente(true);
        when(revisiones.findByPlanificacionIdAndVigenteTrue(planId)).thenReturn(List.of(revision));
        var creada = service.crear(planId, "Corregir horarios");
        prepararResolucion(creada.id(), plan, piso);

        var resultado = service.aprobar(planId, creada.id(), "Autorizado");

        assertThat(resultado.estado()).isEqualTo("APROBADA");
        assertThat(plan.getEstado()).isEqualTo(EstadoPlanificacionAgregada.BORRADOR);
        assertThat(bloque.getEstado()).isEqualTo(EstadoPlanificacion.BORRADOR);
        assertThat(revision.getVigente()).isFalse();
        assertThat(revision.getObservacion()).isEqualTo("Observacion historica");
        assertThat(revision.getRevisadaPorPerfilId()).isNotNull();
    }

    @Test
    void rechazoMantienePlanificacionEnRevision() {
        UUID planId = UUID.randomUUID();
        UUID piso = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = prepararPlan(planId, piso);
        var creada = service.crear(planId, "Corregir horarios");
        prepararResolucion(creada.id(), plan, piso);

        var resultado = service.rechazar(planId, creada.id(), "No procede");

        assertThat(resultado.estado()).isEqualTo("RECHAZADA");
        assertThat(plan.getEstado()).isEqualTo(EstadoPlanificacionAgregada.EN_REVISION);
        verify(revisiones, never()).saveAll(any());
    }

    private PlanificacionAgregadaJpaEntity prepararPlan(UUID planId, UUID... pisos) {
        PlanificacionAgregadaJpaEntity plan = plan(planId, EstadoPlanificacionAgregada.EN_REVISION);
        when(planes.findLockedById(planId)).thenReturn(Optional.of(plan));
        List<PlanificacionJpaEntity> items = new ArrayList<>();
        for (UUID piso : pisos) {
            PlanificacionJpaEntity bloque = bloque(planId, UUID.randomUUID());
            items.add(bloque);
            when(academico.obtenerLaboratorio(bloque.getLaboratorioId())).thenReturn(
                    new LaboratorioExternoResponse(bloque.getLaboratorioId(), piso, true, true, "DISPONIBLE", 1));
            when(usuarios.obtenerAdministradoresPorPiso(piso)).thenReturn(List.of(UUID.randomUUID()));
        }
        when(bloques.findByPlanificacionId(planId)).thenReturn(items);
        return plan;
    }

    private void prepararResolucion(UUID solicitudId, PlanificacionAgregadaJpaEntity plan, UUID piso) {
        when(solicitudes.findLockedById(solicitudId)).thenReturn(Optional.of(solicitudesGuardadas.get(solicitudId)));
        when(planes.findLockedById(plan.getId())).thenReturn(Optional.of(plan));
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_ADMINISTRADOR_PISO")));
        when(ambito.pisoGestionado()).thenReturn(piso);
        when(decisiones.findBySolicitudRetiroIdAndPisoId(solicitudId, piso)).thenReturn(decisionesGuardadas.stream()
                .filter(item -> piso.equals(item.getPisoId())).findFirst());
    }

    private SolicitudRetiroPlanificacionJpaEntity guardarSolicitud(SolicitudRetiroPlanificacionJpaEntity item) {
        asignarId(item);
        solicitudesGuardadas.put(item.getId(), item);
        return item;
    }

    private DecisionRetiroPlanificacionJpaEntity guardarDecision(DecisionRetiroPlanificacionJpaEntity item) {
        asignarId(item);
        decisionesGuardadas.add(item);
        return item;
    }

    private void asignarId(Object item) {
        try {
            Field campo = item.getClass().getDeclaredField("id");
            campo.setAccessible(true);
            if (campo.get(item) == null) campo.set(item, UUID.randomUUID());
            Field creada = item.getClass().getDeclaredField("creadaEn");
            creada.setAccessible(true);
            if (creada.get(item) == null) creada.set(item, Instant.now());
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private PlanificacionAgregadaJpaEntity plan(UUID id, EstadoPlanificacionAgregada estado) {
        PlanificacionAgregadaJpaEntity item = new PlanificacionAgregadaJpaEntity();
        item.setId(id);
        item.setCarreraId(carrera);
        item.setPeriodoId(UUID.randomUUID());
        item.setCoordinadorPerfilId(perfil);
        item.setEstado(estado);
        item.setCreadaEn(Instant.now());
        return item;
    }

    private PlanificacionJpaEntity bloque(UUID planId, UUID laboratorioId) {
        PlanificacionJpaEntity item = new PlanificacionJpaEntity();
        item.setPlanificacionId(planId);
        item.setLaboratorioId(laboratorioId);
        item.setEstado(EstadoPlanificacion.ENVIADA);
        item.setDiaSemana("LUNES");
        item.setHoraInicio(LocalTime.of(7, 30));
        item.setHoraFin(LocalTime.of(8, 30));
        return item;
    }
}
