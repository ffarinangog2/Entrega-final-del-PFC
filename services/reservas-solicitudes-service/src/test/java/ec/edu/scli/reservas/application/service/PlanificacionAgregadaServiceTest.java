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
import ec.edu.scli.reservas.domain.model.EstadoSolicitudCambio;
import ec.edu.scli.reservas.domain.port.out.ActorActualPort;
import ec.edu.scli.reservas.domain.port.out.ContextoInstitucionalPort;
import ec.edu.scli.reservas.infrastructure.persistence.entity.ObservacionRevisionPlanificacionJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.PlanificacionAgregadaJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.PlanificacionJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.RevisionPlanificacionPisoJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.RevisionSolicitudCambioJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.SolicitudCambioPlanificacionJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.repository.ObservacionRevisionPlanificacionJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.PlanificacionAgregadaJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.PlanificacionJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.ReservaSpringDataRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.RevisionPlanificacionPisoJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.RevisionSolicitudCambioJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.SolicitudCambioPlanificacionJpaRepository;
import ec.edu.scli.reservas.presentation.dto.request.ProponerCambioAgregadoRequest;
import ec.edu.scli.reservas.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
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
    private SolicitudCambioPlanificacionJpaRepository solicitudesCambio;
    private RevisionSolicitudCambioJpaRepository revisionesSolicitudCambio;
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
        solicitudesCambio = mock(SolicitudCambioPlanificacionJpaRepository.class);
        revisionesSolicitudCambio = mock(RevisionSolicitudCambioJpaRepository.class);
        service = new PlanificacionAgregadaService(planes, bloques, revisiones, actores, contextos,
                academico, ambito, usuarios, mock(NotificacionService.class),
                observaciones, reservasOperativas, solicitudesCambio, revisionesSolicitudCambio);
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_COORDINADOR")));
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(
                new ContextoInstitucional(true, true, false, false, false, null, List.of(carrera)));
        when(planes.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(planes.findLockedById(any())).thenAnswer(invocation -> planes.findById(invocation.getArgument(0)));
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
        assertThat(captor.getAllValues()).allSatisfy(revision -> {
            assertThat(revision.getRonda()).isEqualTo(1);
            assertThat(revision.getVigente()).isTrue();
            assertThat(revision.getEstado()).isEqualTo(EstadoRevisionPlanificacion.PENDIENTE);
        });
    }

    @Test
    void reenvioConservaHistorialYCreaRondasIndependientesSinDuplicarVigentes() {
        UUID planId = UUID.randomUUID();
        UUID pisoUno = UUID.randomUUID();
        UUID pisoDos = UUID.randomUUID();
        UUID revisor = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = plan(planId, EstadoPlanificacionAgregada.REQUIERE_CAMBIOS);
        PlanificacionJpaEntity bloqueUno = bloque(planId, 2, UUID.randomUUID(), UUID.randomUUID());
        PlanificacionJpaEntity bloqueDos = bloque(planId, 4, UUID.randomUUID(), UUID.randomUUID());
        RevisionPlanificacionPisoJpaEntity rondaUno = revision(planId, pisoUno, 1, true);
        rondaUno.setEstado(EstadoRevisionPlanificacion.RECHAZADA);
        rondaUno.setObservacion("Conservar observacion historica");
        rondaUno.setRevisadaPorPerfilId(revisor);
        Instant actualizada = Instant.parse("2026-08-20T10:15:30Z");
        rondaUno.setActualizadaEn(actualizada);
        RevisionPlanificacionPisoJpaEntity rondaTresPisoDos = revision(planId, pisoDos, 3, false);
        rondaTresPisoDos.setEstado(EstadoRevisionPlanificacion.RECHAZADA);
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(bloques.findByPlanificacionId(planId)).thenReturn(List.of(bloqueUno, bloqueDos));
        when(academico.obtenerLaboratorio(bloqueUno.getLaboratorioId()))
                .thenReturn(laboratorio(bloqueUno, pisoUno));
        when(academico.obtenerLaboratorio(bloqueDos.getLaboratorioId()))
                .thenReturn(laboratorio(bloqueDos, pisoDos));
        when(revisiones.findByPlanificacionId(planId)).thenReturn(List.of(rondaUno, rondaTresPisoDos));

        service.enviar(planId);

        assertThat(rondaUno.getVigente()).isFalse();
        assertThat(rondaUno.getEstado()).isEqualTo(EstadoRevisionPlanificacion.RECHAZADA);
        assertThat(rondaUno.getObservacion()).isEqualTo("Conservar observacion historica");
        assertThat(rondaUno.getRevisadaPorPerfilId()).isEqualTo(revisor);
        assertThat(rondaUno.getActualizadaEn()).isEqualTo(actualizada);
        var captor = org.mockito.ArgumentCaptor.forClass(RevisionPlanificacionPisoJpaEntity.class);
        verify(revisiones, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).filteredOn(item -> pisoUno.equals(item.getPisoId()))
                .singleElement().satisfies(item -> assertThat(item.getRonda()).isEqualTo(2));
        assertThat(captor.getAllValues()).filteredOn(item -> pisoDos.equals(item.getPisoId()))
                .singleElement().satisfies(item -> assertThat(item.getRonda()).isEqualTo(4));
        assertThat(captor.getAllValues()).allSatisfy(item -> {
            assertThat(item.getEstado()).isEqualTo(EstadoRevisionPlanificacion.PENDIENTE);
            assertThat(item.getVigente()).isTrue();
        });
        var orden = inOrder(revisiones);
        orden.verify(revisiones).saveAllAndFlush(List.of(rondaUno));
        orden.verify(revisiones, times(2)).save(any());
    }

    @Test
    void reintentoDeEnvioEnRevisionEsIdempotente() {
        UUID planId = UUID.randomUUID();
        UUID piso = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = plan(planId, EstadoPlanificacionAgregada.BORRADOR);
        PlanificacionJpaEntity bloque = bloque(planId, 2, UUID.randomUUID(), UUID.randomUUID());
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(bloques.findByPlanificacionId(planId)).thenReturn(List.of(bloque));
        when(academico.obtenerLaboratorio(bloque.getLaboratorioId())).thenReturn(laboratorio(bloque, piso));

        service.enviar(planId);
        service.enviar(planId);

        verify(revisiones, times(1)).save(any());
        verify(revisiones, never()).saveAllAndFlush(any());
    }

    @Test
    void planificacionAprobadaNoAbreNuevaRonda() {
        UUID planId = UUID.randomUUID();
        when(planes.findById(planId)).thenReturn(Optional.of(
                plan(planId, EstadoPlanificacionAgregada.APROBADA)));

        assertThatThrownBy(() -> service.enviar(planId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no se encuentra editable");
        verify(revisiones, never()).save(any());
        verify(revisiones, never()).saveAllAndFlush(any());
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
    void retirarPlanificacionEnRevisionExigeSolicitudAutorizada() {
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

        assertThatThrownBy(() -> service.retirar(planId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("solicitar el retiro");
        assertThat(plan.getEstado()).isEqualTo(EstadoPlanificacionAgregada.EN_REVISION);
        assertThat(plan.getEnviadaEn()).isNotNull();
        verify(revisiones, never()).saveAll(any());
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
    void enviarIgnoraBloqueCanceladoYNoLoReactiva() {
        UUID planId = UUID.randomUUID();
        UUID laboratorio = UUID.randomUUID();
        UUID piso = UUID.randomUUID();
        PlanificacionJpaEntity cancelado = bloque(planId, 2, UUID.randomUUID(), laboratorio);
        cancelado.setEstado(EstadoPlanificacion.CANCELADA);
        PlanificacionJpaEntity activo = bloque(planId, 3, UUID.randomUUID(), laboratorio);
        activo.setHoraInicio(cancelado.getHoraInicio());
        activo.setHoraFin(cancelado.getHoraFin());
        when(planes.findById(planId)).thenReturn(Optional.of(
                plan(planId, EstadoPlanificacionAgregada.BORRADOR)));
        when(bloques.findByPlanificacionId(planId)).thenReturn(List.of(cancelado, activo));
        when(academico.obtenerLaboratorio(laboratorio)).thenReturn(laboratorio(activo, piso));

        service.enviar(planId);

        assertThat(activo.getEstado()).isEqualTo(EstadoPlanificacion.ENVIADA);
        assertThat(cancelado.getEstado()).isEqualTo(EstadoPlanificacion.CANCELADA);
        verify(bloques).saveAll(List.of(activo));
    }

    @Test
    void enviarRechazaPlanificacionSinBloquesActivos() {
        UUID planId = UUID.randomUUID();
        PlanificacionJpaEntity cancelado = bloque(planId, 2, UUID.randomUUID(), UUID.randomUUID());
        cancelado.setEstado(EstadoPlanificacion.CANCELADA);
        when(planes.findById(planId)).thenReturn(Optional.of(
                plan(planId, EstadoPlanificacionAgregada.BORRADOR)));
        when(bloques.findByPlanificacionId(planId)).thenReturn(List.of(cancelado));

        assertThatThrownBy(() -> service.enviar(planId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bloques activos");
    }

    @Test
    void permiteLaboratoriosDistintosEnLaMismaFranja() {
        UUID planId = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = plan(planId, EstadoPlanificacionAgregada.BORRADOR);
        PlanificacionJpaEntity primero = bloque(planId, 2, UUID.randomUUID(), UUID.randomUUID());
        PlanificacionJpaEntity segundo = bloque(planId, 3, UUID.randomUUID(), UUID.randomUUID());
        segundo.setHoraInicio(primero.getHoraInicio());
        segundo.setHoraFin(primero.getHoraFin());
        when(academico.obtenerLaboratorio(primero.getLaboratorioId()))
                .thenReturn(laboratorio(primero, UUID.randomUUID()));
        when(academico.obtenerLaboratorio(segundo.getLaboratorioId()))
                .thenReturn(laboratorio(segundo, UUID.randomUUID()));

        service.validarOcupacionOficial(plan, List.of(primero, segundo));
    }

    @Test
    void permiteFranjasContiguasEnElMismoLaboratorio() {
        UUID planId = UUID.randomUUID();
        UUID laboratorio = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = plan(planId, EstadoPlanificacionAgregada.BORRADOR);
        PlanificacionJpaEntity primero = bloque(planId, 2, UUID.randomUUID(), laboratorio);
        primero.setHoraInicio(LocalTime.of(7, 30));
        primero.setHoraFin(LocalTime.of(8, 30));
        PlanificacionJpaEntity segundo = bloque(planId, 3, UUID.randomUUID(), laboratorio);
        segundo.setHoraInicio(LocalTime.of(8, 30));
        segundo.setHoraFin(LocalTime.of(9, 30));
        when(academico.obtenerLaboratorio(laboratorio))
                .thenReturn(laboratorio(primero, UUID.randomUUID()));

        service.validarOcupacionOficial(plan, List.of(primero, segundo));
    }

    @Test
    void rechazaBloquesSolapadosDelMismoNivel() {
        UUID planId = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = plan(planId, EstadoPlanificacionAgregada.BORRADOR);
        PlanificacionJpaEntity primero = bloque(planId, 4, UUID.randomUUID(), UUID.randomUUID());
        PlanificacionJpaEntity segundo = bloque(planId, 4, UUID.randomUUID(), UUID.randomUUID());
        segundo.setHoraInicio(primero.getHoraInicio());
        segundo.setHoraFin(primero.getHoraFin());

        assertThatThrownBy(() -> service.validarOcupacionOficial(plan, List.of(primero, segundo)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nivel");
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
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Guayaquil"));
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_ADMINISTRADOR")));
        when(planes.findAll()).thenReturn(List.of(plan));
        when(bloques.findByPlanificacionId(plan.getId())).thenReturn(List.of());
        when(academico.obtenerPeriodo(plan.getPeriodoId())).thenReturn(new PeriodoExternoResponse(
                plan.getPeriodoId(), "P", "P", hoy.minusMonths(2), hoy.minusDays(1),
                "FINALIZADO", "P", "PPA", 1));
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
        when(revisiones.findByPisoIdAndVigenteTrue(piso)).thenReturn(List.of(propia));
        when(planes.findById(visible.getId())).thenReturn(Optional.of(visible));
        PlanificacionJpaEntity bloque = bloque(visible.getId(), 1, UUID.randomUUID(), UUID.randomUUID());
        PlanificacionJpaEntity cancelado = bloque(visible.getId(), 2, UUID.randomUUID(), UUID.randomUUID());
        cancelado.setEstado(EstadoPlanificacion.CANCELADA);
        PlanificacionJpaEntity ajeno = bloque(visible.getId(), 3, UUID.randomUUID(), UUID.randomUUID());
        when(bloques.findByPlanificacionId(visible.getId())).thenReturn(List.of(bloque, cancelado, ajeno));
        when(academico.obtenerLaboratorio(bloque.getLaboratorioId())).thenReturn(laboratorio(bloque, piso));
        when(academico.obtenerLaboratorio(ajeno.getLaboratorioId())).thenReturn(laboratorio(ajeno, otroPiso));

        var resultado = service.listar();

        assertThat(resultado).singleElement()
                .satisfies(response -> assertThat(response.bloques()).extracting(item -> item.id())
                        .containsExactly(bloque.getId()))
                .extracting(response -> response.id())
                .isEqualTo(visible.getId());
    }

    @Test
    void administradorPisoNoVeRevisionHistoricaNiPlanificacionSinBloquesActivos() {
        UUID piso = UUID.randomUUID();
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil,
                Set.of("ROLE_ADMINISTRADOR_PISO")));
        when(ambito.pisoGestionado()).thenReturn(piso);
        when(revisiones.findByPisoIdAndVigenteTrue(piso)).thenReturn(List.of());

        assertThat(service.listar()).isEmpty();
        verify(revisiones, never()).findAll();
    }

    @Test
    void pisoDestinoConRevisionPendienteYSolicitudGlobalPendienteVePlanificacion() {
        UUID piso = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID solicitudId = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = plan(planId, EstadoPlanificacionAgregada.APROBADA);

        RevisionSolicitudCambioJpaEntity revCambio = new RevisionSolicitudCambioJpaEntity();
        revCambio.setSolicitudId(solicitudId);
        revCambio.setPisoId(piso);
        revCambio.setEstado(EstadoSolicitudCambio.PENDIENTE);

        SolicitudCambioPlanificacionJpaEntity sol = new SolicitudCambioPlanificacionJpaEntity();
        sol.setId(solicitudId);
        sol.setPlanificacionId(planId);
        sol.setEstado(EstadoSolicitudCambio.PENDIENTE);

        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_ADMINISTRADOR_PISO")));
        when(ambito.pisoGestionado()).thenReturn(piso);
        when(revisiones.findByPisoIdAndVigenteTrue(piso)).thenReturn(List.of());
        when(revisionesSolicitudCambio.findByPisoId(piso)).thenReturn(List.of(revCambio));
        when(solicitudesCambio.findById(solicitudId)).thenReturn(Optional.of(sol));
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(bloques.findByPlanificacionId(planId)).thenReturn(List.of());

        var resultado = service.listar();

        assertThat(resultado).singleElement().satisfies(response -> {
            assertThat(response.id()).isEqualTo(planId);
            assertThat(response.bloques()).isEmpty();
            assertThat(response.pisoGestionadoId()).isEqualTo(piso);
        });
    }

    @Test
    void pisoDestinoNoVePlanificacionSiSuRevisionYaFueAprobadaAunqueSolicitudSigaPendiente() {
        UUID piso = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID solicitudId = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = plan(planId, EstadoPlanificacionAgregada.APROBADA);

        RevisionSolicitudCambioJpaEntity revCambio = new RevisionSolicitudCambioJpaEntity();
        revCambio.setSolicitudId(solicitudId);
        revCambio.setPisoId(piso);
        revCambio.setEstado(EstadoSolicitudCambio.APROBADA);

        SolicitudCambioPlanificacionJpaEntity sol = new SolicitudCambioPlanificacionJpaEntity();
        sol.setId(solicitudId);
        sol.setPlanificacionId(planId);
        sol.setEstado(EstadoSolicitudCambio.PENDIENTE);

        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_ADMINISTRADOR_PISO")));
        when(ambito.pisoGestionado()).thenReturn(piso);
        when(revisiones.findByPisoIdAndVigenteTrue(piso)).thenReturn(List.of());
        when(revisionesSolicitudCambio.findByPisoId(piso)).thenReturn(List.of(revCambio));
        when(solicitudesCambio.findById(solicitudId)).thenReturn(Optional.of(sol));
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(bloques.findByPlanificacionId(planId)).thenReturn(List.of());

        var resultado = service.listar();

        assertThat(resultado).isEmpty();
    }

    @Test
    void solicitudGlobalAprobadaNoMantienePlanificacionVisiblePorCondicionB() {
        UUID piso = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID solicitudId = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = plan(planId, EstadoPlanificacionAgregada.APROBADA);

        RevisionSolicitudCambioJpaEntity revCambio = new RevisionSolicitudCambioJpaEntity();
        revCambio.setSolicitudId(solicitudId);
        revCambio.setPisoId(piso);
        revCambio.setEstado(EstadoSolicitudCambio.APROBADA);

        SolicitudCambioPlanificacionJpaEntity sol = new SolicitudCambioPlanificacionJpaEntity();
        sol.setId(solicitudId);
        sol.setPlanificacionId(planId);
        sol.setEstado(EstadoSolicitudCambio.APROBADA);

        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_ADMINISTRADOR_PISO")));
        when(ambito.pisoGestionado()).thenReturn(piso);
        when(revisiones.findByPisoIdAndVigenteTrue(piso)).thenReturn(List.of());
        when(revisionesSolicitudCambio.findByPisoId(piso)).thenReturn(List.of(revCambio));
        when(solicitudesCambio.findById(solicitudId)).thenReturn(Optional.of(sol));
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(bloques.findByPlanificacionId(planId)).thenReturn(List.of());

        var resultado = service.listar();

        assertThat(resultado).isEmpty();
    }

    @Test
    void solicitudGlobalRechazadaNoMantienePlanificacionVisiblePorCondicionB() {
        UUID piso = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID solicitudId = UUID.randomUUID();
        PlanificacionAgregadaJpaEntity plan = plan(planId, EstadoPlanificacionAgregada.APROBADA);

        RevisionSolicitudCambioJpaEntity revCambio = new RevisionSolicitudCambioJpaEntity();
        revCambio.setSolicitudId(solicitudId);
        revCambio.setPisoId(piso);
        revCambio.setEstado(EstadoSolicitudCambio.RECHAZADA);

        SolicitudCambioPlanificacionJpaEntity sol = new SolicitudCambioPlanificacionJpaEntity();
        sol.setId(solicitudId);
        sol.setPlanificacionId(planId);
        sol.setEstado(EstadoSolicitudCambio.RECHAZADA);

        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_ADMINISTRADOR_PISO")));
        when(ambito.pisoGestionado()).thenReturn(piso);
        when(revisiones.findByPisoIdAndVigenteTrue(piso)).thenReturn(List.of());
        when(revisionesSolicitudCambio.findByPisoId(piso)).thenReturn(List.of(revCambio));
        when(solicitudesCambio.findById(solicitudId)).thenReturn(Optional.of(sol));
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(bloques.findByPlanificacionId(planId)).thenReturn(List.of());

        var resultado = service.listar();

        assertThat(resultado).isEmpty();
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

    private RevisionPlanificacionPisoJpaEntity revision(UUID planId, UUID pisoId, int ronda, boolean vigente) {
        RevisionPlanificacionPisoJpaEntity revision = new RevisionPlanificacionPisoJpaEntity();
        revision.setId(UUID.randomUUID());
        revision.setPlanificacionId(planId);
        revision.setPisoId(pisoId);
        revision.setRonda(ronda);
        revision.setVigente(vigente);
        return revision;
    }
}
