package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.dto.LaboratorioExternoResponse;
import ec.edu.scli.reservas.domain.model.ActorAutenticado;
import ec.edu.scli.reservas.domain.model.ContextoInstitucional;
import ec.edu.scli.reservas.domain.model.EstadoPlanificacion;
import ec.edu.scli.reservas.domain.port.out.ActorActualPort;
import ec.edu.scli.reservas.domain.port.out.ContextoInstitucionalPort;
import ec.edu.scli.reservas.domain.port.out.DocenteInstitucionalPort;
import ec.edu.scli.reservas.infrastructure.persistence.entity.PlanificacionJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.repository.PlanificacionJpaRepository;
import ec.edu.scli.reservas.presentation.dto.request.GuardarPlanificacionRequest;
import ec.edu.scli.reservas.presentation.dto.request.ProponerPlanificacionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlanificacionServiceTest {
    private final UUID perfil = UUID.randomUUID();
    private final UUID carrera = UUID.randomUUID();
    private final UUID laboratorio = UUID.randomUUID();
    private PlanificacionJpaRepository repository;
    private ActorActualPort actores;
    private ContextoInstitucionalPort contextos;
    private PoliticaAmbitoLaboratorio ambito;
    private AcademicoLaboratoriosClient academico;
    private NotificacionService notificaciones;
    private PlanificacionService service;
    private DocenteInstitucionalPort docentes;

    @BeforeEach
    void preparar() {
        repository = mock(PlanificacionJpaRepository.class);
        actores = mock(ActorActualPort.class);
        contextos = mock(ContextoInstitucionalPort.class);
        ambito = mock(PoliticaAmbitoLaboratorio.class);
        academico = mock(AcademicoLaboratoriosClient.class);
        notificaciones = mock(NotificacionService.class);
        docentes = mock(DocenteInstitucionalPort.class);
        service = new PlanificacionService(repository, actores, contextos, ambito, academico, notificaciones, docentes);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(academico.existePeriodoLectivo(any())).thenReturn(true);
        when(academico.obtenerContextoMateria(any())).thenAnswer(i ->
                new ec.edu.scli.reservas.client.dto.MateriaContextoExternoResponse(i.getArgument(0), carrera, true, true));
        when(docentes.obtenerPorDocenteId(any())).thenAnswer(i ->
                new ec.edu.scli.reservas.domain.model.DocenteInstitucional(i.getArgument(0), UUID.randomUUID(), true));
        when(academico.obtenerLaboratorio(any())).thenReturn(
                new LaboratorioExternoResponse(laboratorio, UUID.randomUUID(), true, true, "ACTIVO", 30));
        coordinador(carrera);
    }

    @Test
    void creaEditaEnviaYReenviaBorradorDeSuCarrera() {
        var creado = service.crear(request(carrera, laboratorio));
        assertThat(creado.estado()).isEqualTo("BORRADOR");
        PlanificacionJpaEntity entity = capturarGuardado();
        entity.setId(UUID.randomUUID());
        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));
        assertThat(service.editar(entity.getId(), request(carrera, laboratorio)).estado()).isEqualTo("BORRADOR");
        assertThat(service.enviar(entity.getId()).estado()).isEqualTo("ENVIADA");
        entity.setEstado(EstadoPlanificacion.PROPUESTA_CAMBIO);
        assertThat(service.reenviar(entity.getId()).estado()).isEqualTo("ENVIADA");
    }

    @Test
    void coordinadorNoOperaOtraCarrera() {
        assertThatThrownBy(() -> service.crear(request(UUID.randomUUID(), laboratorio)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void administradorPisoAceptaRechazaYProponeSoloEnSuPiso() {
        PlanificacionJpaEntity entity = entity(EstadoPlanificacion.ENVIADA);
        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_ADMINISTRADOR_PISO")));
        when(repository.bloquearConflictos(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        assertThat(service.aceptar(entity.getId()).estado()).isEqualTo("CONFIRMADA");
        verify(ambito).validarGestion(laboratorio);
        verify(notificaciones).notificarPerfil(eq(perfil), eq("Planificacion aceptada"), anyString(), anyMap());

        entity.setEstado(EstadoPlanificacion.ENVIADA);
        assertThat(service.rechazar(entity.getId(), "sin capacidad").estado()).isEqualTo("RECHAZADA");
        verify(notificaciones).notificarPerfil(eq(perfil), eq("Planificacion rechazada"), anyString(), anyMap());
        entity.setEstado(EstadoPlanificacion.ENVIADA);
        UUID alternativo = UUID.randomUUID();
        assertThat(service.proponer(entity.getId(), new ProponerPlanificacionRequest(
                alternativo, LocalTime.of(10, 0), LocalTime.of(12, 0), "alternativa")).estado())
                .isEqualTo("PROPUESTA_CAMBIO");
        verify(ambito).validarGestion(alternativo);
        verify(notificaciones).notificarPerfil(eq(perfil), eq("Propuesta de planificacion"), anyString(), anyMap());
    }

    @Test
    void propuestaSoloLaAceptaCoordinadorDeCarreraYDetectaConflicto() {
        PlanificacionJpaEntity entity = entity(EstadoPlanificacion.PROPUESTA_CAMBIO);
        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(repository.bloquearConflictos(any(), any(), any(), any(), any(), any())).thenReturn(List.of(entity(EstadoPlanificacion.CONFIRMADA)));
        assertThatThrownBy(() -> service.aceptarPropuesta(entity.getId()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("ocupada");
        entity.setEstado(EstadoPlanificacion.PROPUESTA_CAMBIO);
        when(repository.bloquearConflictos(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        assertThat(service.aceptarPropuesta(entity.getId()).estado()).isEqualTo("CONFIRMADA");
    }

    @Test
    void cancelaEstadosPermitidosYRechazaTransicionInvalida() {
        PlanificacionJpaEntity entity = entity(EstadoPlanificacion.BORRADOR);
        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));
        assertThat(service.cancelar(entity.getId()).estado()).isEqualTo("CANCELADA");
        assertThatThrownBy(() -> service.enviar(entity.getId())).isInstanceOf(IllegalStateException.class);
    }

    private void coordinador(UUID carreraId) {
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_COORDINADOR")));
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(
                new ContextoInstitucional(true, true, false, false, false, null, List.of(carreraId)));
    }

    private GuardarPlanificacionRequest request(UUID carreraId, UUID laboratorioId) {
        return new GuardarPlanificacionRequest(UUID.randomUUID(), carreraId, UUID.randomUUID(), UUID.randomUUID(),
                laboratorioId, "LUNES", LocalTime.of(8, 0), LocalTime.of(10, 0), "plan semestral");
    }

    private PlanificacionJpaEntity capturarGuardado() {
        var captor = org.mockito.ArgumentCaptor.forClass(PlanificacionJpaEntity.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private PlanificacionJpaEntity entity(EstadoPlanificacion estado) {
        PlanificacionJpaEntity entity = new PlanificacionJpaEntity();
        entity.setId(UUID.randomUUID()); entity.setPeriodoId(UUID.randomUUID()); entity.setCarreraId(carrera);
        entity.setMateriaId(UUID.randomUUID()); entity.setLaboratorioId(laboratorio); entity.setDiaSemana("LUNES");
        entity.setHoraInicio(LocalTime.of(8, 0)); entity.setHoraFin(LocalTime.of(10, 0)); entity.setEstado(estado);
        entity.setCreadoPorPerfilId(perfil); entity.setCreadaEn(Instant.now()); entity.setActualizadaEn(Instant.now());
        return entity;
    }
}
