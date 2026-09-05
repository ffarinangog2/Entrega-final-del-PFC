package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.UsuariosClient;
import ec.edu.scli.reservas.client.dto.DocenteExternoResponse;
import ec.edu.scli.reservas.client.dto.LaboratorioExternoResponse;
import ec.edu.scli.reservas.client.dto.PeriodoExternoResponse;
import ec.edu.scli.reservas.domain.model.ActorAutenticado;
import ec.edu.scli.reservas.domain.model.ContextoInstitucional;
import ec.edu.scli.reservas.domain.model.EstadoPlanificacion;
import ec.edu.scli.reservas.domain.model.EstadoPlanificacionAgregada;
import ec.edu.scli.reservas.domain.model.EstadoSolicitudCambio;
import ec.edu.scli.reservas.domain.model.TipoSolicitudCambio;
import ec.edu.scli.reservas.domain.port.out.ActorActualPort;
import ec.edu.scli.reservas.domain.port.out.ContextoInstitucionalPort;
import ec.edu.scli.reservas.infrastructure.persistence.entity.PlanificacionAgregadaJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.PlanificacionJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.RevisionSolicitudCambioJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.SolicitudCambioPlanificacionJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.repository.PlanificacionAgregadaJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.PlanificacionJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.RevisionSolicitudCambioJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.SolicitudCambioPlanificacionJpaRepository;
import ec.edu.scli.reservas.presentation.dto.request.CrearSolicitudCambioRequest;
import ec.edu.scli.reservas.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

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

class SolicitudCambioPlanificacionServiceTest {
    private SolicitudCambioPlanificacionJpaRepository solicitudes = mock(SolicitudCambioPlanificacionJpaRepository.class);
    private RevisionSolicitudCambioJpaRepository revisiones = mock(RevisionSolicitudCambioJpaRepository.class);
    private PlanificacionAgregadaJpaRepository planes = mock(PlanificacionAgregadaJpaRepository.class);
    private PlanificacionJpaRepository bloques = mock(PlanificacionJpaRepository.class);
    private ActorActualPort actores = mock(ActorActualPort.class);
    private ContextoInstitucionalPort contextos = mock(ContextoInstitucionalPort.class);
    private PoliticaAmbitoLaboratorio ambito = mock(PoliticaAmbitoLaboratorio.class);
    private AcademicoLaboratoriosClient academico = mock(AcademicoLaboratoriosClient.class);
    private UsuariosClient usuarios = mock(UsuariosClient.class);
    private NotificacionService notificaciones = mock(NotificacionService.class);
    private PlanificacionAgregadaService disponibilidad = mock(PlanificacionAgregadaService.class);
    private SolicitudCambioPlanificacionService service = new SolicitudCambioPlanificacionService(
            solicitudes, revisiones, planes, bloques, actores, contextos, ambito, academico, usuarios, notificaciones, disponibilidad);

    private final UUID perfil = UUID.randomUUID();
    private final UUID carrera = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();
    private final UUID bloqueId = UUID.randomUUID();
    private final UUID periodo = UUID.randomUUID();
    private final UUID labOrigen = UUID.randomUUID();
    private final UUID labDestino = UUID.randomUUID();
    private final UUID pisoOrigen = UUID.randomUUID();
    private final UUID pisoDestino = UUID.randomUUID();
    private final UUID docenteOrigen = UUID.randomUUID();
    private final UUID docenteDestino = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_COORDINADOR")));
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(new ContextoInstitucional(true, true, false, false, false, null, List.of(carrera)));
        when(academico.obtenerPeriodo(periodo)).thenReturn(new PeriodoExternoResponse(periodo, "P", "P",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(10), "ACTIVO", "PPA", "REGULAR PPA", 1));
        when(academico.obtenerLaboratorio(labOrigen)).thenReturn(new LaboratorioExternoResponse(labOrigen, pisoOrigen, true, true, "DISPONIBLE", 20));
        when(academico.obtenerLaboratorio(labDestino)).thenReturn(new LaboratorioExternoResponse(labDestino, pisoDestino, true, true, "DISPONIBLE", 20));
        when(usuarios.obtenerAdministradoresPorPiso(any())).thenReturn(List.of(UUID.randomUUID()));
    }

    private PlanificacionAgregadaJpaEntity crearPlan(EstadoPlanificacionAgregada estado) {
        var plan = new PlanificacionAgregadaJpaEntity();
        plan.setId(planId);
        plan.setCarreraId(carrera);
        plan.setPeriodoId(periodo);
        plan.setCoordinadorPerfilId(perfil);
        plan.setEstado(estado);
        return plan;
    }

    private PlanificacionJpaEntity crearBloque() {
        var b = new PlanificacionJpaEntity();
        b.setId(bloqueId);
        b.setPlanificacionId(planId);
        b.setLaboratorioId(labOrigen);
        b.setDocenteId(docenteOrigen);
        b.setDiaSemana("LUNES");
        b.setHoraInicio(LocalTime.of(8, 0));
        b.setHoraFin(LocalTime.of(9, 0));
        b.setNivel(5);
        b.setEstado(EstadoPlanificacion.BORRADOR);
        return b;
    }

    @Test
    void solicitudPendienteConservaBloqueOriginalYProtegePisosOrigenDestino() {
        var plan = crearPlan(EstadoPlanificacionAgregada.APROBADA);
        var bloque = crearBloque();
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(bloques.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(solicitudes.saveAndFlush(any())).thenAnswer(i -> {
            SolicitudCambioPlanificacionJpaEntity s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(revisiones.findBySolicitudId(any())).thenReturn(List.of());

        var out = service.crear(planId, new CrearSolicitudCambioRequest(
                bloqueId, TipoSolicitudCambio.LABORATORIO, "Mantenimiento imprevisto", labDestino, null, null, null, null));

        assertThat(out.estado()).isEqualTo("PENDIENTE");
        assertThat(bloque.getLaboratorioId()).isEqualTo(labOrigen);
        verify(revisiones, times(2)).save(any());
        verify(bloques, never()).save(any());
    }

    @Test
    void crearRechazaSiNoEsCoordinador() {
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_DOCENTE")));
        assertThatThrownBy(() -> service.crear(planId, new CrearSolicitudCambioRequest(bloqueId, TipoSolicitudCambio.CANCELACION, "Motivo", null, null, null, null, null)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Solo el coordinador");
    }

    @Test
    void crearRechazaSiContextoNoContieneCarrera() {
        var plan = crearPlan(EstadoPlanificacionAgregada.APROBADA);
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(new ContextoInstitucional(true, true, false, false, false, null, List.of(UUID.randomUUID())));

        assertThatThrownBy(() -> service.crear(planId, new CrearSolicitudCambioRequest(bloqueId, TipoSolicitudCambio.CANCELACION, "Motivo", null, null, null, null, null)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("no pertenece a su carrera");
    }

    @Test
    void crearRechazaSiPlanNoEstaAprobada() {
        var plan = crearPlan(EstadoPlanificacionAgregada.BORRADOR);
        when(planes.findById(planId)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.crear(planId, new CrearSolicitudCambioRequest(bloqueId, TipoSolicitudCambio.CANCELACION, "Motivo", null, null, null, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo una planificacion aprobada");
    }

    @Test
    void crearRechazaSiPeriodoEsHistorico() {
        var plan = crearPlan(EstadoPlanificacionAgregada.APROBADA);
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(academico.obtenerPeriodo(periodo)).thenReturn(new PeriodoExternoResponse(periodo, "P", "P",
                LocalDate.now().minusMonths(2), LocalDate.now().minusDays(1), "CERRADO", "PPA", "PPA", 1));

        assertThatThrownBy(() -> service.crear(planId, new CrearSolicitudCambioRequest(bloqueId, TipoSolicitudCambio.CANCELACION, "Motivo", null, null, null, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("finalizada es historica");
    }

    @Test
    void crearRechazaSiBloqueNoPerteneceAPlan() {
        var plan = crearPlan(EstadoPlanificacionAgregada.APROBADA);
        var bloque = crearBloque();
        bloque.setPlanificacionId(UUID.randomUUID());
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(bloques.findById(bloqueId)).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> service.crear(planId, new CrearSolicitudCambioRequest(bloqueId, TipoSolicitudCambio.CANCELACION, "Motivo", null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece a la planificacion");
    }

    @Test
    void crearRechazaSiBloqueTieneSolicitudPendiente() {
        var plan = crearPlan(EstadoPlanificacionAgregada.APROBADA);
        var bloque = crearBloque();
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(bloques.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(solicitudes.existsByBloqueIdAndEstado(bloqueId, EstadoSolicitudCambio.PENDIENTE)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(planId, new CrearSolicitudCambioRequest(bloqueId, TipoSolicitudCambio.CANCELACION, "Motivo", null, null, null, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya tiene una solicitud pendiente");
    }

    @Test
    void crearValidaPropuestasPorTipo() {
        var plan = crearPlan(EstadoPlanificacionAgregada.APROBADA);
        var bloque = crearBloque();
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(bloques.findById(bloqueId)).thenReturn(Optional.of(bloque));

        // LABORATORIO: sin id o mismo laboratorio
        assertThatThrownBy(() -> service.crear(planId, new CrearSolicitudCambioRequest(bloqueId, TipoSolicitudCambio.LABORATORIO, "Motivo", null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Debe proponer otro laboratorio");
        assertThatThrownBy(() -> service.crear(planId, new CrearSolicitudCambioRequest(bloqueId, TipoSolicitudCambio.LABORATORIO, "Motivo", labOrigen, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Debe proponer otro laboratorio");

        // DOCENTE: sin id o mismo docente
        assertThatThrownBy(() -> service.crear(planId, new CrearSolicitudCambioRequest(bloqueId, TipoSolicitudCambio.DOCENTE, "Motivo", null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Debe proponer otro docente");
        assertThatThrownBy(() -> service.crear(planId, new CrearSolicitudCambioRequest(bloqueId, TipoSolicitudCambio.DOCENTE, "Motivo", null, docenteOrigen, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Debe proponer otro docente");

        // HORARIO: horario inconsistente o nulo
        assertThatThrownBy(() -> service.crear(planId, new CrearSolicitudCambioRequest(bloqueId, TipoSolicitudCambio.HORARIO, "Motivo", null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("horario valido");
        assertThatThrownBy(() -> service.crear(planId, new CrearSolicitudCambioRequest(bloqueId, TipoSolicitudCambio.HORARIO, "Motivo", null, null, "MARTES", LocalTime.of(10, 0), LocalTime.of(9, 0))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("horario valido");
    }

    @Test
    void crearSoportaTipoDocenteYHorario() {
        var plan = crearPlan(EstadoPlanificacionAgregada.APROBADA);
        var bloque = crearBloque();
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(bloques.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(solicitudes.saveAndFlush(any())).thenAnswer(i -> {
            SolicitudCambioPlanificacionJpaEntity s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        // Crear tipo DOCENTE
        var reqDocente = new CrearSolicitudCambioRequest(bloqueId, TipoSolicitudCambio.DOCENTE, "Cambio de docente", null, docenteDestino, null, null, null);
        var resDocente = service.crear(planId, reqDocente);
        assertThat(resDocente.docentePropuestoId()).isEqualTo(docenteDestino);

        // Crear tipo HORARIO
        var reqHorario = new CrearSolicitudCambioRequest(bloqueId, TipoSolicitudCambio.HORARIO, "Ajuste horario", null, null, "MIERCOLES", LocalTime.of(10, 0), LocalTime.of(12, 0));
        var resHorario = service.crear(planId, reqHorario);
        assertThat(resHorario.diaPropuesto()).isEqualTo("MIERCOLES");
        assertThat(resHorario.horaInicioPropuesta()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    void crearFallaSiNoHayAdminAsignadoAlPiso() {
        var plan = crearPlan(EstadoPlanificacionAgregada.APROBADA);
        var bloque = crearBloque();
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(bloques.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(usuarios.obtenerAdministradoresPorPiso(pisoOrigen)).thenReturn(List.of());
        when(solicitudes.saveAndFlush(any())).thenAnswer(i -> {
            SolicitudCambioPlanificacionJpaEntity s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        assertThatThrownBy(() -> service.crear(planId, new CrearSolicitudCambioRequest(bloqueId, TipoSolicitudCambio.CANCELACION, "Cancelacion", null, null, null, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No existe administrador asignado al piso");
    }

    @Test
    void listarFiltraPorRol() {
        var sol = new SolicitudCambioPlanificacionJpaEntity();
        sol.setId(UUID.randomUUID());
        sol.setPlanificacionId(planId);
        sol.setTipo(TipoSolicitudCambio.CANCELACION);
        sol.setEstado(EstadoSolicitudCambio.PENDIENTE);
        sol.setMotivo("Test");
        when(solicitudes.findByPlanificacionIdOrderByCreadaEnDesc(planId)).thenReturn(List.of(sol));

        // Administrador global
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_ADMINISTRADOR")));
        assertThat(service.listar(planId)).hasSize(1);

        // Administrador de piso con revision en su piso
        UUID pisoAdmin = UUID.randomUUID();
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_ADMINISTRADOR_PISO")));
        when(ambito.pisoGestionado()).thenReturn(pisoAdmin);
        when(revisiones.findBySolicitudIdAndPisoId(sol.getId(), pisoAdmin)).thenReturn(Optional.of(new RevisionSolicitudCambioJpaEntity()));
        assertThat(service.listar(planId)).hasSize(1);

        // Coordinador de carrera
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_COORDINADOR")));
        var plan = crearPlan(EstadoPlanificacionAgregada.APROBADA);
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(new ContextoInstitucional(true, true, false, false, false, null, List.of(carrera)));
        assertThat(service.listar(planId)).hasSize(1);

        // Coordinador de otra carrera -> AccessDenied
        when(contextos.obtenerPorPerfilId(perfil)).thenReturn(new ContextoInstitucional(true, true, false, false, false, null, List.of(UUID.randomUUID())));
        assertThatThrownBy(() -> service.listar(planId)).isInstanceOf(AccessDeniedException.class);

        // Rol no autorizado
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_ESTUDIANTE")));
        assertThatThrownBy(() -> service.listar(planId)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aprobarCompletaYParcial() {
        UUID solId = UUID.randomUUID();
        UUID piso1 = UUID.randomUUID();
        UUID piso2 = UUID.randomUUID();

        var sol = new SolicitudCambioPlanificacionJpaEntity();
        sol.setId(solId);
        sol.setPlanificacionId(planId);
        sol.setBloqueId(bloqueId);
        sol.setTipo(TipoSolicitudCambio.CANCELACION);
        sol.setEstado(EstadoSolicitudCambio.PENDIENTE);
        sol.setMotivo("Cancelacion justificada");

        var rev1 = new RevisionSolicitudCambioJpaEntity();
        rev1.setSolicitudId(solId);
        rev1.setPisoId(piso1);
        rev1.setEstado(EstadoSolicitudCambio.PENDIENTE);

        var rev2 = new RevisionSolicitudCambioJpaEntity();
        rev2.setSolicitudId(solId);
        rev2.setPisoId(piso2);
        rev2.setEstado(EstadoSolicitudCambio.PENDIENTE);

        when(solicitudes.findById(solId)).thenReturn(Optional.of(sol));
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_ADMINISTRADOR_PISO")));
        when(ambito.pisoGestionado()).thenReturn(piso1);
        when(revisiones.findBySolicitudIdAndPisoId(solId, piso1)).thenReturn(Optional.of(rev1));
        when(revisiones.findBySolicitudId(solId)).thenReturn(List.of(rev1, rev2));

        // Aprobacion parcial (piso 1 aprueba, pero piso 2 aun esta pendiente)
        var respParcial = service.aprobar(solId, "Aprobado por piso 1");
        assertThat(respParcial.estado()).isEqualTo("PENDIENTE");
        assertThat(rev1.getEstado()).isEqualTo(EstadoSolicitudCambio.APROBADA);
        verify(bloques, never()).saveAndFlush(any());

        // Aprobacion final (piso 2 tambien aprueba)
        rev2.setEstado(EstadoSolicitudCambio.APROBADA);
        var plan = crearPlan(EstadoPlanificacionAgregada.APROBADA);
        var bloque = crearBloque();
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(bloques.findById(bloqueId)).thenReturn(Optional.of(bloque));

        var respFinal = service.aprobar(solId, "Aprobado completamente");
        assertThat(sol.getEstado()).isEqualTo(EstadoSolicitudCambio.APROBADA);
        assertThat(bloque.getEstado()).isEqualTo(EstadoPlanificacion.CANCELADA);
        verify(solicitudes).save(sol);
    }

    @Test
    void aprobarAplicaModificacionDeDocente() {
        UUID solId = UUID.randomUUID();
        UUID piso = UUID.randomUUID();

        var sol = new SolicitudCambioPlanificacionJpaEntity();
        sol.setId(solId);
        sol.setPlanificacionId(planId);
        sol.setBloqueId(bloqueId);
        sol.setTipo(TipoSolicitudCambio.DOCENTE);
        sol.setDocenteAnteriorId(docenteOrigen);
        sol.setDocentePropuestoId(docenteDestino);
        sol.setLaboratorioPropuestoId(labOrigen);
        sol.setDiaPropuesto("LUNES");
        sol.setHoraInicioPropuesta(LocalTime.of(8, 0));
        sol.setHoraFinPropuesta(LocalTime.of(10, 0));
        sol.setEstado(EstadoSolicitudCambio.PENDIENTE);
        sol.setMotivo("Asignacion docente");

        var rev = new RevisionSolicitudCambioJpaEntity();
        rev.setSolicitudId(solId);
        rev.setPisoId(piso);
        rev.setEstado(EstadoSolicitudCambio.PENDIENTE);

        when(solicitudes.findById(solId)).thenReturn(Optional.of(sol));
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_ADMINISTRADOR_PISO")));
        when(ambito.pisoGestionado()).thenReturn(piso);
        when(revisiones.findBySolicitudIdAndPisoId(solId, piso)).thenReturn(Optional.of(rev));
        when(revisiones.findBySolicitudId(solId)).thenReturn(List.of(rev));

        var plan = crearPlan(EstadoPlanificacionAgregada.APROBADA);
        var bloque = crearBloque();
        when(planes.findById(planId)).thenReturn(Optional.of(plan));
        when(bloques.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(bloques.findByPlanificacionId(planId)).thenReturn(List.of(bloque));

        // Docente no pertenece a la carrera -> IllegalStateException
        when(usuarios.docentePerteneceCarrera(docenteDestino, carrera)).thenReturn(false);
        assertThatThrownBy(() -> service.aprobar(solId, "Ok"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("docente propuesto no pertenece");

        // Docente pertenece a la carrera -> exito y notificaciones enviadas
        when(usuarios.docentePerteneceCarrera(docenteDestino, carrera)).thenReturn(true);
        when(usuarios.obtenerDocentePorId(docenteOrigen)).thenReturn(new DocenteExternoResponse(docenteOrigen, UUID.randomUUID(), true));
        when(usuarios.obtenerDocentePorId(docenteDestino)).thenReturn(new DocenteExternoResponse(docenteDestino, UUID.randomUUID(), true));
        when(usuarios.obtenerEstudiantesCompatibles(carrera, periodo, bloque.getNivel())).thenReturn(List.of(UUID.randomUUID()));

        service.aprobar(solId, "Aprobado");
        assertThat(bloque.getDocenteId()).isEqualTo(docenteDestino);
        verify(disponibilidad).validarOcupacionOficial(eq(plan), any());
    }

    @Test
    void rechazarActualizaEstadoYNotifica() {
        UUID solId = UUID.randomUUID();
        UUID piso = UUID.randomUUID();

        var sol = new SolicitudCambioPlanificacionJpaEntity();
        sol.setId(solId);
        sol.setPlanificacionId(planId);
        sol.setTipo(TipoSolicitudCambio.DOCENTE);
        sol.setEstado(EstadoSolicitudCambio.PENDIENTE);
        sol.setMotivo("Cambio");

        var rev = new RevisionSolicitudCambioJpaEntity();
        rev.setSolicitudId(solId);
        rev.setPisoId(piso);
        rev.setEstado(EstadoSolicitudCambio.PENDIENTE);

        when(solicitudes.findById(solId)).thenReturn(Optional.of(sol));
        when(actores.obtener()).thenReturn(new ActorAutenticado(perfil, Set.of("ROLE_ADMINISTRADOR_PISO")));
        when(ambito.pisoGestionado()).thenReturn(piso);
        when(revisiones.findBySolicitudIdAndPisoId(solId, piso)).thenReturn(Optional.of(rev));
        var plan = crearPlan(EstadoPlanificacionAgregada.APROBADA);
        when(planes.findById(planId)).thenReturn(Optional.of(plan));

        // Rechazo sin observacion -> IllegalArgumentException
        assertThatThrownBy(() -> service.rechazar(solId, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requiere observacion");

        // Rechazo exitoso
        var res = service.rechazar(solId, "No hay disponibilidad");
        assertThat(res.estado()).isEqualTo("RECHAZADA");
        assertThat(sol.getEstado()).isEqualTo(EstadoSolicitudCambio.RECHAZADA);
        assertThat(rev.getEstado()).isEqualTo(EstadoSolicitudCambio.RECHAZADA);
        verify(solicitudes).save(sol);
        verify(notificaciones).notificarPerfilIdempotente(eq(plan.getCoordinadorPerfilId()), any(), any(), any(), any());
    }

    @Test
    void operacionFallaSiSolicitudNoExisteOYaFueResuelta() {
        UUID inexitente = UUID.randomUUID();
        when(solicitudes.findById(inexitente)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.aprobar(inexitente, "Ok")).isInstanceOf(ResourceNotFoundException.class);

        UUID resueltaId = UUID.randomUUID();
        var sol = new SolicitudCambioPlanificacionJpaEntity();
        sol.setId(resueltaId);
        sol.setEstado(EstadoSolicitudCambio.APROBADA);
        when(solicitudes.findById(resueltaId)).thenReturn(Optional.of(sol));
        assertThatThrownBy(() -> service.aprobar(resueltaId, "Ok")).isInstanceOf(IllegalStateException.class);
    }
}
