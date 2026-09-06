package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.UsuariosClient;
import ec.edu.scli.reservas.domain.model.*;
import ec.edu.scli.reservas.domain.port.out.ActorActualPort;
import ec.edu.scli.reservas.domain.port.out.ContextoInstitucionalPort;
import ec.edu.scli.reservas.infrastructure.persistence.entity.*;
import ec.edu.scli.reservas.infrastructure.persistence.repository.*;
import ec.edu.scli.reservas.presentation.dto.response.SolicitudRetiroResponse;
import ec.edu.scli.reservas.presentation.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class SolicitudRetiroPlanificacionService {
    private final SolicitudRetiroPlanificacionJpaRepository solicitudes;
    private final DecisionRetiroPlanificacionJpaRepository decisiones;
    private final PlanificacionAgregadaJpaRepository planes;
    private final PlanificacionJpaRepository bloques;
    private final RevisionPlanificacionPisoJpaRepository revisiones;
    private final ActorActualPort actores;
    private final ContextoInstitucionalPort contextos;
    private final PoliticaAmbitoLaboratorio ambito;
    private final AcademicoLaboratoriosClient academico;
    private final UsuariosClient usuarios;
    private final NotificacionService notificaciones;

    public SolicitudRetiroPlanificacionService(SolicitudRetiroPlanificacionJpaRepository solicitudes,
            DecisionRetiroPlanificacionJpaRepository decisiones, PlanificacionAgregadaJpaRepository planes,
            PlanificacionJpaRepository bloques, RevisionPlanificacionPisoJpaRepository revisiones,
            ActorActualPort actores, ContextoInstitucionalPort contextos, PoliticaAmbitoLaboratorio ambito,
            AcademicoLaboratoriosClient academico, UsuariosClient usuarios, NotificacionService notificaciones) {
        this.solicitudes = solicitudes;
        this.decisiones = decisiones;
        this.planes = planes;
        this.bloques = bloques;
        this.revisiones = revisiones;
        this.actores = actores;
        this.contextos = contextos;
        this.ambito = ambito;
        this.academico = academico;
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
    }

    @Transactional
    public SolicitudRetiroResponse crear(UUID planificacionId, String motivo) {
        ActorAutenticado actor = actores.obtener();
        if (!actor.tiene("ROLE_COORDINADOR")) throw new AccessDeniedException("Solo el coordinador puede solicitar el retiro");
        if (motivo == null || motivo.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
        PlanificacionAgregadaJpaEntity plan = planes.findLockedById(planificacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Planificacion no encontrada"));
        validarCarrera(actor, plan);
        if (plan.getEstado() != EstadoPlanificacionAgregada.EN_REVISION) {
            throw new IllegalStateException("Solo una planificacion en revision admite solicitud de retiro");
        }
        var pendiente = solicitudes.findByPlanificacionIdAndEstado(planificacionId, EstadoSolicitudRetiro.PENDIENTE);
        if (pendiente.isPresent()) return response(pendiente.get());
        Set<UUID> pisos = pisosActivos(planificacionId);
        if (pisos.isEmpty()) throw new IllegalStateException("La planificacion no tiene pisos afectados activos");
        SolicitudRetiroPlanificacionJpaEntity solicitud = new SolicitudRetiroPlanificacionJpaEntity();
        solicitud.setPlanificacionId(planificacionId);
        solicitud.setSolicitantePerfilId(actor.perfilId());
        solicitud.setMotivo(motivo.trim());
        solicitud = solicitudes.saveAndFlush(solicitud);
        for (UUID pisoId : pisos) {
            DecisionRetiroPlanificacionJpaEntity decision = new DecisionRetiroPlanificacionJpaEntity();
            decision.setSolicitudRetiroId(solicitud.getId());
            decision.setPisoId(pisoId);
            decisiones.save(decision);
            List<UUID> administradores = usuarios.obtenerAdministradoresPorPiso(pisoId);
            if (administradores.isEmpty()) throw new IllegalStateException("No existe administrador asignado al piso " + pisoId);
            UUID solicitudId = solicitud.getId();
            administradores.forEach(perfilId -> notificaciones.notificarPerfilIdempotente(perfilId,
                    "RETIRO:NUEVO:" + solicitudId + ":" + pisoId + ":" + perfilId,
                    "Solicitud de retiro pendiente", motivo.trim(),
                    Map.of("tipo", "SOLICITUD_RETIRO", "referenciaId", solicitudId.toString(),
                            "planificacionId", planificacionId.toString())));
        }
        decisiones.flush();
        return response(solicitud);
    }

    @Transactional(readOnly = true)
    public List<SolicitudRetiroResponse> listar(UUID planificacionId) {
        ActorAutenticado actor = actores.obtener();
        PlanificacionAgregadaJpaEntity plan = plan(planificacionId);
        List<SolicitudRetiroPlanificacionJpaEntity> items =
                solicitudes.findByPlanificacionIdOrderByCreadaEnDesc(planificacionId);
        if (actor.tiene("ROLE_COORDINADOR")) validarCarrera(actor, plan);
        else if (actor.tiene("ROLE_ADMINISTRADOR_PISO")) {
            UUID pisoId = ambito.pisoGestionado();
            items = items.stream().filter(item -> decisiones
                    .findBySolicitudRetiroIdAndPisoId(item.getId(), pisoId).isPresent()).toList();
        } else if (!actor.tiene("ROLE_ADMINISTRADOR")) {
            throw new AccessDeniedException("No puede consultar solicitudes de retiro");
        }
        return items.stream().map(this::response).toList();
    }

    @Transactional
    public SolicitudRetiroResponse aprobar(UUID planificacionId, UUID solicitudId, String observacion) {
        return resolver(planificacionId, solicitudId, observacion, EstadoSolicitudRetiro.APROBADA);
    }

    @Transactional
    public SolicitudRetiroResponse rechazar(UUID planificacionId, UUID solicitudId, String observacion) {
        return resolver(planificacionId, solicitudId, observacion, EstadoSolicitudRetiro.RECHAZADA);
    }

    private SolicitudRetiroResponse resolver(UUID planificacionId, UUID solicitudId, String observacion,
            EstadoSolicitudRetiro resultado) {
        SolicitudRetiroPlanificacionJpaEntity solicitud = solicitudes.findLockedById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de retiro no encontrada"));
        if (!planificacionId.equals(solicitud.getPlanificacionId())) throw new IllegalArgumentException("La solicitud no pertenece a la planificacion");
        if (solicitud.getEstado() != EstadoSolicitudRetiro.PENDIENTE) return response(solicitud);
        PlanificacionAgregadaJpaEntity plan = planes.findLockedById(planificacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Planificacion no encontrada"));
        if (plan.getEstado() != EstadoPlanificacionAgregada.EN_REVISION) throw new IllegalStateException("La planificacion ya no esta en revision");
        ActorAutenticado actor = actores.obtener();
        UUID pisoId = ambito.pisoGestionado();
        DecisionRetiroPlanificacionJpaEntity decision = decisiones
                .findBySolicitudRetiroIdAndPisoId(solicitudId, pisoId)
                .orElseThrow(() -> new AccessDeniedException("La solicitud no corresponde a su piso"));
        if (decision.getEstado() != EstadoSolicitudRetiro.PENDIENTE) return response(solicitud);
        Instant ahora = Instant.now();
        decision.setEstado(resultado);
        decision.setRevisadaPorPerfilId(actor.perfilId());
        decision.setObservacion(observacion == null || observacion.isBlank() ? null : observacion.trim());
        decision.setResueltaEn(ahora);
        decisiones.saveAndFlush(decision);
        if (resultado == EstadoSolicitudRetiro.RECHAZADA) {
            solicitud.setEstado(EstadoSolicitudRetiro.RECHAZADA);
            solicitud.setResueltaEn(ahora);
            solicitudes.save(solicitud);
            notificarCoordinador(plan, solicitud, "Solicitud de retiro rechazada");
        } else if (decisiones.findBySolicitudRetiroId(solicitudId).stream()
                .allMatch(item -> item.getEstado() == EstadoSolicitudRetiro.APROBADA)) {
            autorizarRetiro(plan, solicitud, ahora);
        }
        return response(solicitud);
    }

    private void autorizarRetiro(PlanificacionAgregadaJpaEntity plan,
            SolicitudRetiroPlanificacionJpaEntity solicitud, Instant ahora) {
        solicitud.setEstado(EstadoSolicitudRetiro.APROBADA);
        solicitud.setResueltaEn(ahora);
        solicitudes.save(solicitud);
        List<RevisionPlanificacionPisoJpaEntity> actuales =
                revisiones.findByPlanificacionIdAndVigenteTrue(plan.getId());
        actuales.forEach(item -> item.setVigente(false));
        revisiones.saveAll(actuales);
        List<PlanificacionJpaEntity> items = bloques.findByPlanificacionId(plan.getId());
        items.stream().filter(item -> item.getEstado() == EstadoPlanificacion.ENVIADA)
                .forEach(item -> item.setEstado(EstadoPlanificacion.BORRADOR));
        bloques.saveAll(items);
        plan.setEstado(EstadoPlanificacionAgregada.BORRADOR);
        plan.setEnviadaEn(null);
        plan.setActualizadaEn(ahora);
        planes.save(plan);
        notificarCoordinador(plan, solicitud, "Retiro autorizado: la planificacion volvio a borrador");
    }

    private Set<UUID> pisosActivos(UUID planificacionId) {
        Set<UUID> pisos = new LinkedHashSet<>();
        bloques.findByPlanificacionId(planificacionId).stream()
                .filter(item -> item.getEstado() != EstadoPlanificacion.CANCELADA)
                .forEach(item -> pisos.add(academico.obtenerLaboratorio(item.getLaboratorioId()).pisoId()));
        return pisos;
    }

    private void notificarCoordinador(PlanificacionAgregadaJpaEntity plan,
            SolicitudRetiroPlanificacionJpaEntity solicitud, String titulo) {
        notificaciones.notificarPerfilIdempotente(plan.getCoordinadorPerfilId(),
                "RETIRO:RESUELTO:" + solicitud.getId() + ":" + solicitud.getEstado(), titulo,
                solicitud.getMotivo(), Map.of("tipo", "SOLICITUD_RETIRO_RESUELTA",
                        "referenciaId", solicitud.getId().toString(),
                        "planificacionId", plan.getId().toString()));
    }

    private void validarCarrera(ActorAutenticado actor, PlanificacionAgregadaJpaEntity plan) {
        var contexto = contextos.obtenerPorPerfilId(actor.perfilId());
        if (contexto == null || !contexto.carreraIds().contains(plan.getCarreraId())) {
            throw new AccessDeniedException("La planificacion no pertenece a su carrera");
        }
    }

    private PlanificacionAgregadaJpaEntity plan(UUID id) {
        return planes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Planificacion no encontrada"));
    }

    private SolicitudRetiroResponse response(SolicitudRetiroPlanificacionJpaEntity solicitud) {
        List<SolicitudRetiroResponse.Decision> detalle = decisiones.findBySolicitudRetiroId(solicitud.getId())
                .stream().map(item -> new SolicitudRetiroResponse.Decision(item.getPisoId(),
                        item.getEstado().name(), item.getRevisadaPorPerfilId(), item.getObservacion(),
                        item.getCreadaEn(), item.getResueltaEn())).toList();
        long aprobados = detalle.stream().filter(item -> "APROBADA".equals(item.estado())).count();
        return new SolicitudRetiroResponse(solicitud.getId(), solicitud.getPlanificacionId(),
                solicitud.getSolicitantePerfilId(), solicitud.getMotivo(), solicitud.getEstado().name(),
                solicitud.getCreadaEn(), solicitud.getResueltaEn(), aprobados, detalle.size(), detalle);
    }
}
