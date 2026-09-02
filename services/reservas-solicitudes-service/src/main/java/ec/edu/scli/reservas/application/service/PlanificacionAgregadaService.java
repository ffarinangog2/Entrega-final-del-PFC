package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.UsuariosClient;
import ec.edu.scli.reservas.domain.model.ActorAutenticado;
import ec.edu.scli.reservas.domain.model.EstadoPlanificacionAgregada;
import ec.edu.scli.reservas.domain.model.EstadoRevisionPlanificacion;
import ec.edu.scli.reservas.domain.port.out.ActorActualPort;
import ec.edu.scli.reservas.domain.port.out.ContextoInstitucionalPort;
import ec.edu.scli.reservas.infrastructure.persistence.entity.PlanificacionAgregadaJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.PlanificacionJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.RevisionPlanificacionPisoJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.ObservacionRevisionPlanificacionJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.repository.PlanificacionAgregadaJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.PlanificacionJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.RevisionPlanificacionPisoJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.ObservacionRevisionPlanificacionJpaRepository;
import ec.edu.scli.reservas.presentation.dto.request.ProponerCambioAgregadoRequest;
import ec.edu.scli.reservas.presentation.dto.response.PlanificacionAgregadaResponse;
import ec.edu.scli.reservas.presentation.dto.response.PlanificacionResponse;
import ec.edu.scli.reservas.presentation.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PlanificacionAgregadaService {
    private final PlanificacionAgregadaJpaRepository planes;
    private final PlanificacionJpaRepository bloques;
    private final RevisionPlanificacionPisoJpaRepository revisiones;
    private final ActorActualPort actores;
    private final ContextoInstitucionalPort contextos;
    private final AcademicoLaboratoriosClient academico;
    private final PoliticaAmbitoLaboratorio ambitoLaboratorio;
    private final UsuariosClient usuarios;
    private final NotificacionService notificaciones;
    private final ObservacionRevisionPlanificacionJpaRepository observaciones;

    public PlanificacionAgregadaService(PlanificacionAgregadaJpaRepository planes,
            PlanificacionJpaRepository bloques, RevisionPlanificacionPisoJpaRepository revisiones,
            ActorActualPort actores, ContextoInstitucionalPort contextos,
            AcademicoLaboratoriosClient academico, PoliticaAmbitoLaboratorio ambitoLaboratorio,
            UsuariosClient usuarios, NotificacionService notificaciones,
            ObservacionRevisionPlanificacionJpaRepository observaciones) {
        this.planes = planes;
        this.bloques = bloques;
        this.revisiones = revisiones;
        this.actores = actores;
        this.contextos = contextos;
        this.academico = academico;
        this.ambitoLaboratorio = ambitoLaboratorio;
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
        this.observaciones = observaciones;
    }

    @Transactional
    public PlanificacionAgregadaResponse iniciar(UUID periodoId) {
        ActorAutenticado actor = coordinador();
        UUID carreraId = carrera(actor);
        if (!academico.existePeriodoLectivo(periodoId)) {
            throw new IllegalArgumentException("El ciclo academico no existe");
        }
        var existente = planes.findByCarreraIdAndPeriodoId(carreraId, periodoId);
        if (existente.isPresent()) return map(existente.get());
        PlanificacionAgregadaJpaEntity plan = new PlanificacionAgregadaJpaEntity();
        plan.setCarreraId(carreraId);
        plan.setPeriodoId(periodoId);
        plan.setCoordinadorPerfilId(actor.perfilId());
        plan.setEstado(EstadoPlanificacionAgregada.BORRADOR);
        plan.setCreadaEn(Instant.now());
        plan.setActualizadaEn(Instant.now());
        try {
            return map(planes.saveAndFlush(plan));
        } catch (DataIntegrityViolationException exception) {
            return map(planes.findByCarreraIdAndPeriodoId(carreraId, periodoId).orElseThrow(() -> exception));
        }
    }

    @Transactional(readOnly = true)
    public List<PlanificacionAgregadaResponse> listar() {
        ActorAutenticado actor = actores.obtener();
        if (actor.tiene("ROLE_COORDINADOR")) {
            return planes.findByCarreraIdOrderByCreadaEnDesc(carrera(actor)).stream().map(this::map).toList();
        }
        if (actor.tiene("ROLE_ADMINISTRADOR")) return planes.findAll().stream().map(this::map).toList();
        if (actor.tiene("ROLE_ADMINISTRADOR_PISO")) {
            UUID pisoId = ambitoLaboratorio.pisoGestionado();
            return revisiones.findAll().stream().filter(item -> pisoId.equals(item.getPisoId()))
                    .map(item -> planes.findById(item.getPlanificacionId()).orElse(null))
                    .filter(java.util.Objects::nonNull).distinct().map(this::mapParaPiso).toList();
        }
        throw new AccessDeniedException("No puede consultar planificaciones agregadas");
    }

    @Transactional
    public PlanificacionAgregadaResponse enviar(UUID id) {
        PlanificacionAgregadaJpaEntity plan = propia(id);
        if (plan.getEstado() != EstadoPlanificacionAgregada.BORRADOR
                && plan.getEstado() != EstadoPlanificacionAgregada.REQUIERE_CAMBIOS) {
            throw new IllegalStateException("La planificacion no se encuentra editable");
        }
        List<PlanificacionJpaEntity> items = bloques.findByPlanificacionId(id);
        if (items.isEmpty()) throw new IllegalStateException("La planificacion no contiene bloques");
        validarConflictos(items);
        Set<UUID> pisos = new LinkedHashSet<>();
        for (PlanificacionJpaEntity item : items) {
            var laboratorio = academico.obtenerLaboratorio(item.getLaboratorioId());
            if (laboratorio == null || !laboratorio.existe() || !laboratorio.activo()) {
                throw new IllegalStateException("Un laboratorio de la planificacion no esta disponible");
            }
            pisos.add(laboratorio.pisoId());
        }
        Instant ahora = Instant.now();
        List<RevisionPlanificacionPisoJpaEntity> anteriores = revisiones.findByPlanificacionId(id);
        anteriores.forEach(item -> observaciones.deleteByRevisionId(item.getId()));
        revisiones.deleteAll(anteriores);
        for (UUID pisoId : pisos) {
            RevisionPlanificacionPisoJpaEntity revision = new RevisionPlanificacionPisoJpaEntity();
            revision.setPlanificacionId(id);
            revision.setPisoId(pisoId);
            revision.setEstado(EstadoRevisionPlanificacion.PENDIENTE);
            revision.setCreadaEn(ahora);
            revision.setActualizadaEn(ahora);
            revisiones.save(revision);
            usuarios.obtenerAdministradoresPorPiso(pisoId).forEach(perfilId ->
                    notificaciones.notificarPerfil(perfilId,
                            "Nueva planificacion academica pendiente de revision",
                            "Revise la planificacion completa correspondiente a su piso",
                            java.util.Map.of("tipo", "PLANIFICACION", "planificacionId", id.toString())));
        }
        plan.setEstado(EstadoPlanificacionAgregada.EN_REVISION);
        plan.setEnviadaEn(ahora);
        plan.setActualizadaEn(ahora);
        return map(planes.saveAndFlush(plan));
    }

    @Transactional
    public PlanificacionAgregadaResponse retirar(UUID id) {
        PlanificacionAgregadaJpaEntity plan = propia(id);
        if (plan.getEstado() != EstadoPlanificacionAgregada.EN_REVISION) {
            throw new IllegalStateException("Solo puede retirarse una planificacion en revision");
        }
        List<RevisionPlanificacionPisoJpaEntity> actuales = revisiones.findByPlanificacionId(id);
        if (actuales.stream().anyMatch(item ->
                item.getEstado() != EstadoRevisionPlanificacion.PENDIENTE)) {
            throw new IllegalStateException("La revision ya fue atendida por un piso");
        }
        actuales.forEach(item -> observaciones.deleteByRevisionId(item.getId()));
        revisiones.deleteAll(actuales);
        plan.setEstado(EstadoPlanificacionAgregada.BORRADOR);
        plan.setEnviadaEn(null);
        plan.setActualizadaEn(Instant.now());
        return map(planes.saveAndFlush(plan));
    }

    @Transactional
    public PlanificacionAgregadaResponse aprobarPiso(UUID id) {
        ActorAutenticado actor = actores.obtener();
        UUID pisoId = ambitoLaboratorio.pisoGestionado();
        RevisionPlanificacionPisoJpaEntity revision = revisiones.findByPlanificacionIdAndPisoId(id, pisoId)
                .orElseThrow(() -> new AccessDeniedException("La planificacion no corresponde a su piso"));
        revision.setEstado(EstadoRevisionPlanificacion.APROBADA);
        revision.setRevisadaPorPerfilId(actor.perfilId());
        revision.setActualizadaEn(Instant.now());
        revisiones.saveAndFlush(revision);
        PlanificacionAgregadaJpaEntity plan = obtener(id);
        if (revisiones.findByPlanificacionId(id).stream()
                .allMatch(item -> item.getEstado() == EstadoRevisionPlanificacion.APROBADA)) {
            plan.setEstado(EstadoPlanificacionAgregada.APROBADA);
            plan.setAprobadaEn(Instant.now());
            plan.setActualizadaEn(Instant.now());
            planes.saveAndFlush(plan);
        }
        return mapParaPiso(plan);
    }

    @Transactional
    public PlanificacionAgregadaResponse rechazarPiso(UUID id, String observacion) {
        if (observacion == null || observacion.isBlank()) {
            throw new IllegalArgumentException("El rechazo requiere una observacion");
        }
        PlanificacionAgregadaJpaEntity plan = revisarMiPiso(id, EstadoRevisionPlanificacion.RECHAZADA,
                observacion);
        return mapParaPiso(plan);
    }

    @Transactional
    public PlanificacionAgregadaResponse proponerCambio(UUID id, ProponerCambioAgregadoRequest request) {
        UUID pisoId = ambitoLaboratorio.pisoGestionado();
        RevisionPlanificacionPisoJpaEntity revision = revisiones.findByPlanificacionIdAndPisoId(id, pisoId)
                .orElseThrow(() -> new AccessDeniedException("La planificacion no corresponde a su piso"));
        PlanificacionJpaEntity bloque = bloques.findById(request.bloqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Bloque no encontrado"));
        if (!id.equals(bloque.getPlanificacionId())
                || !pisoId.equals(academico.obtenerLaboratorio(bloque.getLaboratorioId()).pisoId())) {
            throw new AccessDeniedException("El bloque no corresponde a su piso");
        }
        if (request.laboratorioPropuestoId() != null) {
            ambitoLaboratorio.validarGestion(request.laboratorioPropuestoId());
        }
        ObservacionRevisionPlanificacionJpaEntity detalle = new ObservacionRevisionPlanificacionJpaEntity();
        detalle.setRevisionId(revision.getId());
        detalle.setBloqueId(bloque.getId());
        detalle.setLaboratorioPropuestoId(request.laboratorioPropuestoId());
        detalle.setObservacion(request.observacion());
        detalle.setCreadaEn(Instant.now());
        observaciones.save(detalle);
        PlanificacionAgregadaJpaEntity plan = revisarMiPiso(id,
                EstadoRevisionPlanificacion.PROPUESTA_CAMBIO, request.observacion());
        return mapParaPiso(plan);
    }

    private PlanificacionAgregadaJpaEntity revisarMiPiso(UUID id, EstadoRevisionPlanificacion estado,
            String observacion) {
        ActorAutenticado actor = actores.obtener();
        UUID pisoId = ambitoLaboratorio.pisoGestionado();
        RevisionPlanificacionPisoJpaEntity revision = revisiones.findByPlanificacionIdAndPisoId(id, pisoId)
                .orElseThrow(() -> new AccessDeniedException("La planificacion no corresponde a su piso"));
        revision.setEstado(estado);
        revision.setObservacion(observacion);
        revision.setRevisadaPorPerfilId(actor.perfilId());
        revision.setActualizadaEn(Instant.now());
        revisiones.saveAndFlush(revision);
        PlanificacionAgregadaJpaEntity plan = obtener(id);
        plan.setEstado(EstadoPlanificacionAgregada.REQUIERE_CAMBIOS);
        plan.setActualizadaEn(Instant.now());
        return planes.saveAndFlush(plan);
    }

    private void validarConflictos(List<PlanificacionJpaEntity> items) {
        for (int left = 0; left < items.size(); left++) {
            PlanificacionJpaEntity primero = items.get(left);
            validarNivel(primero.getNivel());
            for (int right = left + 1; right < items.size(); right++) {
                PlanificacionJpaEntity segundo = items.get(right);
                if (!primero.getDiaSemana().equals(segundo.getDiaSemana())
                        || primero.getHoraInicio().compareTo(segundo.getHoraFin()) >= 0
                        || primero.getHoraFin().compareTo(segundo.getHoraInicio()) <= 0) continue;
                if (primero.getLaboratorioId().equals(segundo.getLaboratorioId())) {
                    throw new IllegalStateException("Un laboratorio tiene bloques solapados entre niveles");
                }
                if (primero.getDocenteId() != null && primero.getDocenteId().equals(segundo.getDocenteId())) {
                    throw new IllegalStateException("Un docente tiene bloques solapados entre niveles");
                }
            }
        }
    }

    private void validarNivel(Integer nivel) {
        if (nivel == null || nivel < 1 || nivel > 10) throw new IllegalArgumentException("El nivel debe estar entre 1 y 10");
    }

    private PlanificacionAgregadaJpaEntity propia(UUID id) {
        PlanificacionAgregadaJpaEntity plan = obtener(id);
        ActorAutenticado actor = coordinador();
        if (!carrera(actor).equals(plan.getCarreraId())) throw new AccessDeniedException("La carrera no pertenece al coordinador");
        return plan;
    }

    private PlanificacionAgregadaJpaEntity obtener(UUID id) {
        return planes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Planificacion no encontrada"));
    }

    private ActorAutenticado coordinador() {
        ActorAutenticado actor = actores.obtener();
        if (!actor.tiene("ROLE_COORDINADOR")) throw new AccessDeniedException("Solo un coordinador puede operar este flujo");
        return actor;
    }

    private UUID carrera(ActorAutenticado actor) {
        var contexto = contextos.obtenerPorPerfilId(actor.perfilId());
        if (contexto == null || !contexto.perfilExiste() || !contexto.perfilActivo()
                || contexto.carreraIds().size() != 1) {
            throw new AccessDeniedException("El coordinador no posee una carrera institucional unica y activa");
        }
        return contexto.carreraIds().getFirst();
    }

    private PlanificacionAgregadaResponse map(PlanificacionAgregadaJpaEntity plan) {
        return response(plan, bloques.findByPlanificacionId(plan.getId()));
    }

    private PlanificacionAgregadaResponse mapParaPiso(PlanificacionAgregadaJpaEntity plan) {
        UUID pisoId = ambitoLaboratorio.pisoGestionado();
        return response(plan, bloques.findByPlanificacionId(plan.getId()).stream()
                .filter(item -> pisoId.equals(academico.obtenerLaboratorio(item.getLaboratorioId()).pisoId())).toList());
    }

    private PlanificacionAgregadaResponse response(PlanificacionAgregadaJpaEntity plan,
            List<PlanificacionJpaEntity> items) {
        List<PlanificacionResponse> mapped = items.stream().sorted(Comparator
                        .comparing(PlanificacionJpaEntity::getNivel)
                        .thenComparing(PlanificacionJpaEntity::getDiaSemana)
                        .thenComparing(PlanificacionJpaEntity::getHoraInicio))
                .map(this::mapBlock).toList();
        var reviews = revisiones.findByPlanificacionId(plan.getId()).stream()
                .map(item -> new PlanificacionAgregadaResponse.RevisionResponse(item.getId(), item.getPisoId(),
                        item.getEstado().name(), item.getObservacion(), observaciones.findByRevisionId(item.getId())
                                .stream().map(value -> new PlanificacionAgregadaResponse.ObservacionResponse(
                                        value.getBloqueId(), value.getLaboratorioPropuestoId(),
                                        value.getObservacion())).toList()))
                .toList();
        return new PlanificacionAgregadaResponse(plan.getId(), plan.getCarreraId(), plan.getPeriodoId(),
                plan.getEstado().name(), plan.getCoordinadorPerfilId(), plan.getCreadaEn(), plan.getEnviadaEn(),
                plan.getAprobadaEn(), mapped, reviews);
    }

    private PlanificacionResponse mapBlock(PlanificacionJpaEntity item) {
        return new PlanificacionResponse(item.getId(), item.getPlanificacionId(), item.getNivel(), item.getPeriodoId(),
                item.getCarreraId(), item.getMateriaId(), item.getDocenteId(), item.getLaboratorioId(),
                item.getDiaSemana(), item.getHoraInicio(), item.getHoraFin(), item.getEstado().name(),
                item.getObservacion(), item.getCreadoPorPerfilId(), item.getCreadaEn(), item.getActualizadaEn(),
                item.getVersion());
    }
}
