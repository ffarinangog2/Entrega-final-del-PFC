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
import ec.edu.scli.reservas.infrastructure.persistence.repository.ReservaSpringDataRepository;
import ec.edu.scli.reservas.presentation.dto.request.ProponerCambioAgregadoRequest;
import ec.edu.scli.reservas.presentation.dto.response.PlanificacionAgregadaResponse;
import ec.edu.scli.reservas.presentation.dto.response.PlanificacionResponse;
import ec.edu.scli.reservas.presentation.dto.response.DisponibilidadPlanificacionResponse;
import ec.edu.scli.reservas.presentation.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import ec.edu.scli.reservas.domain.model.EstadoPlanificacion;

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
    private final ReservaSpringDataRepository reservasOperativas;

    public PlanificacionAgregadaService(PlanificacionAgregadaJpaRepository planes,
            PlanificacionJpaRepository bloques, RevisionPlanificacionPisoJpaRepository revisiones,
            ActorActualPort actores, ContextoInstitucionalPort contextos,
            AcademicoLaboratoriosClient academico, PoliticaAmbitoLaboratorio ambitoLaboratorio,
            UsuariosClient usuarios, NotificacionService notificaciones,
            ObservacionRevisionPlanificacionJpaRepository observaciones,
            ReservaSpringDataRepository reservasOperativas) {
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
        this.reservasOperativas = reservasOperativas;
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

    @Transactional(readOnly = true)
    public DisponibilidadPlanificacionResponse disponibilidad(UUID planificacionId, UUID periodoId,
            String dia, LocalTime horaInicio, LocalTime horaFin) {
        coordinador();
        if (horaInicio == null || horaFin == null || !horaInicio.isBefore(horaFin))
            throw new IllegalArgumentException("La franja horaria no es valida");
        var ocupacion = bloques.buscarOcupacionGlobal(planificacionId, periodoId, dia.toUpperCase(), horaInicio,
                horaFin, List.of(EstadoPlanificacionAgregada.EN_REVISION, EstadoPlanificacionAgregada.APROBADA));
        return new DisponibilidadPlanificacionResponse(
                ocupacion.stream().map(PlanificacionJpaEntity::getDocenteId).filter(java.util.Objects::nonNull).distinct().toList(),
                ocupacion.stream().map(PlanificacionJpaEntity::getLaboratorioId).distinct().toList());
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
        validarOcupacionOficial(plan, items);
        Set<UUID> pisos = new LinkedHashSet<>();
        for (PlanificacionJpaEntity item : items) {
            var laboratorio = academico.obtenerLaboratorio(item.getLaboratorioId());
            if (laboratorio == null || !laboratorio.existe() || !laboratorio.activo()
                    || !"DISPONIBLE".equalsIgnoreCase(laboratorio.estado())) {
                throw new IllegalStateException("Un laboratorio de la planificacion no esta disponible");
            }
            pisos.add(laboratorio.pisoId());
        }
        Instant ahora = Instant.now();
        List<RevisionPlanificacionPisoJpaEntity> anteriores = revisiones.findByPlanificacionId(id);
        int ronda = anteriores.stream().map(RevisionPlanificacionPisoJpaEntity::getRonda)
                .filter(java.util.Objects::nonNull).max(Integer::compareTo).orElse(0) + 1;
        anteriores.stream().filter(item -> Boolean.TRUE.equals(item.getVigente())).forEach(item -> item.setVigente(false));
        revisiones.saveAll(anteriores);
        for (UUID pisoId : pisos) {
            var administradores = usuarios.obtenerAdministradoresPorPiso(pisoId);
            if (administradores.isEmpty()) {
                throw new IllegalStateException("No existe administrador asignado al piso " + pisoId);
            }
            RevisionPlanificacionPisoJpaEntity revision = new RevisionPlanificacionPisoJpaEntity();
            revision.setPlanificacionId(id);
            revision.setPisoId(pisoId);
            revision.setEstado(EstadoRevisionPlanificacion.PENDIENTE);
            revision.setCreadaEn(ahora);
            revision.setActualizadaEn(ahora);
            revision.setRonda(ronda);
            revision.setVigente(true);
            revisiones.save(revision);
            administradores.forEach(perfilId ->
                    notificaciones.notificarPerfil(perfilId,
                            "Nueva planificacion academica pendiente de revision",
                            "Revise la planificacion completa correspondiente a su piso",
                            java.util.Map.of("tipo", "PLANIFICACION", "planificacionId", id.toString())));
        }
        plan.setEstado(EstadoPlanificacionAgregada.EN_REVISION);
        items.forEach(item -> item.setEstado(EstadoPlanificacion.ENVIADA));
        bloques.saveAll(items);
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
        List<RevisionPlanificacionPisoJpaEntity> actuales = revisiones.findByPlanificacionIdAndVigenteTrue(id);
        if (actuales.stream().anyMatch(item ->
                item.getEstado() != EstadoRevisionPlanificacion.PENDIENTE)) {
            throw new IllegalStateException("La revision ya fue atendida por un piso");
        }
        actuales.forEach(item -> item.setVigente(false));
        revisiones.saveAll(actuales);
        bloques.findByPlanificacionId(id).stream().filter(item -> item.getEstado() == EstadoPlanificacion.ENVIADA)
                .forEach(item -> item.setEstado(EstadoPlanificacion.BORRADOR));
        plan.setEstado(EstadoPlanificacionAgregada.BORRADOR);
        plan.setEnviadaEn(null);
        plan.setActualizadaEn(Instant.now());
        return map(planes.saveAndFlush(plan));
    }

    @Transactional
    public PlanificacionAgregadaResponse aprobarPiso(UUID id) {
        ActorAutenticado actor = actores.obtener();
        UUID pisoId = ambitoLaboratorio.pisoGestionado();
        RevisionPlanificacionPisoJpaEntity revision = revisiones.findByPlanificacionIdAndPisoIdAndVigenteTrue(id, pisoId)
                .orElseThrow(() -> new AccessDeniedException("La planificacion no corresponde a su piso"));
        revision.setEstado(EstadoRevisionPlanificacion.APROBADA);
        revision.setRevisadaPorPerfilId(actor.perfilId());
        revision.setActualizadaEn(Instant.now());
        revisiones.saveAndFlush(revision);
        PlanificacionAgregadaJpaEntity plan = obtener(id);
        if (revisiones.findByPlanificacionIdAndVigenteTrue(id).stream()
                .allMatch(item -> item.getEstado() == EstadoRevisionPlanificacion.APROBADA)) {
            List<PlanificacionJpaEntity> items = bloques.findByPlanificacionId(id);
            validarConflictosGlobales(plan, items);
            plan.setEstado(EstadoPlanificacionAgregada.APROBADA);
            plan.setAprobadaEn(Instant.now());
            plan.setActualizadaEn(Instant.now());
            planes.saveAndFlush(plan);
            items.stream().filter(item -> item.getEstado() != EstadoPlanificacion.CANCELADA)
                    .forEach(item -> item.setEstado(EstadoPlanificacion.CONFIRMADA));
            bloques.saveAll(items);
            notificaciones.notificarPerfil(plan.getCoordinadorPerfilId(), "Planificacion aprobada",
                    "Todos los pisos aprobaron la planificacion academica", java.util.Map.of("tipo", "PLANIFICACION_APROBADA", "planificacionId", id.toString()));
            items.stream().map(PlanificacionJpaEntity::getDocenteId).filter(java.util.Objects::nonNull).distinct()
                    .map(usuarios::obtenerDocentePorId).filter(java.util.Objects::nonNull).filter(d -> d.activo())
                    .forEach(d -> notificaciones.notificarPerfil(d.perfilId(), "Horario academico disponible",
                            "Una planificacion aprobada actualizo su horario", java.util.Map.of("tipo", "HORARIO_DOCENTE", "planificacionId", id.toString())));
        }
        return mapParaPiso(plan);
    }

    private void validarConflictosGlobales(PlanificacionAgregadaJpaEntity plan,
            List<PlanificacionJpaEntity> items) {
        List<EstadoPlanificacionAgregada> ocupantes = List.of(
                EstadoPlanificacionAgregada.EN_REVISION,
                EstadoPlanificacionAgregada.APROBADA);
        for (PlanificacionJpaEntity item : items) {
            List<PlanificacionJpaEntity> conflictos = bloques.bloquearConflictosGlobales(
                    plan.getId(), plan.getPeriodoId(), item.getDocenteId(), item.getLaboratorioId(),
                    item.getDiaSemana(), item.getHoraInicio(), item.getHoraFin(), ocupantes);
            if (conflictos.stream().anyMatch(other -> item.getLaboratorioId().equals(other.getLaboratorioId()))) {
                throw new IllegalStateException("El laboratorio ya se encuentra ocupado en esta franja");
            }
            if (item.getDocenteId() != null && conflictos.stream()
                    .anyMatch(other -> item.getDocenteId().equals(other.getDocenteId()))) {
                throw new IllegalStateException("El docente ya se encuentra asignado en esta franja");
            }
        }
    }

    /** Autoridad unica de disponibilidad para envio y cambios posteriores. */
    public void validarOcupacionOficial(PlanificacionAgregadaJpaEntity plan, List<PlanificacionJpaEntity> items) {
        validarConflictos(items);
        validarConflictosGlobales(plan, items);
        validarReservasOperativas(plan, items);
        for (PlanificacionJpaEntity item : items) {
            var laboratorio = academico.obtenerLaboratorio(item.getLaboratorioId());
            if (laboratorio == null || !laboratorio.existe() || !laboratorio.activo()
                    || !"DISPONIBLE".equalsIgnoreCase(laboratorio.estado())) {
                throw new IllegalStateException("El laboratorio no esta operativo");
            }
        }
    }

    private void validarReservasOperativas(PlanificacionAgregadaJpaEntity plan, List<PlanificacionJpaEntity> items) {
        var periodo = academico.obtenerPeriodo(plan.getPeriodoId());
        if (periodo == null || periodo.fechaInicio() == null || periodo.fechaFin() == null) return;
        for (PlanificacionJpaEntity item : items) {
            for (LocalDate fecha = periodo.fechaInicio(); !fecha.isAfter(periodo.fechaFin()); fecha = fecha.plusDays(1)) {
                if (dia(fecha).equals(item.getDiaSemana()) && reservasOperativas.contarConflictosActivos(
                        item.getLaboratorioId(), fecha, item.getHoraInicio(), item.getHoraFin()) > 0) {
                    throw new IllegalStateException("El laboratorio tiene una reserva operativa durante el periodo planificado");
                }
            }
        }
    }

    private String dia(LocalDate fecha) {
        return switch (fecha.getDayOfWeek()) {
            case MONDAY -> "LUNES"; case TUESDAY -> "MARTES"; case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES"; case FRIDAY -> "VIERNES"; case SATURDAY -> "SABADO"; case SUNDAY -> "DOMINGO";
        };
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
        RevisionPlanificacionPisoJpaEntity revision = revisiones.findByPlanificacionIdAndPisoIdAndVigenteTrue(id, pisoId)
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
        RevisionPlanificacionPisoJpaEntity revision = revisiones.findByPlanificacionIdAndPisoIdAndVigenteTrue(id, pisoId)
                .orElseThrow(() -> new AccessDeniedException("La planificacion no corresponde a su piso"));
        revision.setEstado(estado);
        revision.setObservacion(observacion);
        revision.setRevisadaPorPerfilId(actor.perfilId());
        revision.setActualizadaEn(Instant.now());
        revisiones.saveAndFlush(revision);
        PlanificacionAgregadaJpaEntity plan = obtener(id);
        plan.setEstado(EstadoPlanificacionAgregada.REQUIERE_CAMBIOS);
        bloques.findByPlanificacionId(id).stream().filter(item -> item.getEstado() != EstadoPlanificacion.CANCELADA)
                .forEach(item -> item.setEstado(EstadoPlanificacion.PROPUESTA_CAMBIO));
        plan.setActualizadaEn(Instant.now());
        notificaciones.notificarPerfil(plan.getCoordinadorPerfilId(), "Planificacion devuelta",
                observacion, java.util.Map.of("tipo", "PLANIFICACION_DEVUELTA", "planificacionId", id.toString()));
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
                        item.getEstado().name(), item.getObservacion(), item.getRonda(), Boolean.TRUE.equals(item.getVigente()),
                        item.getRevisadaPorPerfilId(), item.getActualizadaEn(), observaciones.findByRevisionId(item.getId())
                                .stream().map(value -> new PlanificacionAgregadaResponse.ObservacionResponse(
                                        value.getBloqueId(), value.getLaboratorioPropuestoId(),
                                        value.getObservacion())).toList()))
                .toList();
        return new PlanificacionAgregadaResponse(plan.getId(), plan.getCarreraId(), plan.getPeriodoId(),
                estadoEfectivo(plan).name(), plan.getCoordinadorPerfilId(), plan.getCreadaEn(), plan.getEnviadaEn(),
                plan.getAprobadaEn(), mapped, reviews);
    }

    private EstadoPlanificacionAgregada estadoEfectivo(PlanificacionAgregadaJpaEntity plan) {
        if (plan.getEstado() != EstadoPlanificacionAgregada.APROBADA) return plan.getEstado();
        var periodo = academico.obtenerPeriodo(plan.getPeriodoId());
        return periodo != null && periodo.fechaFin() != null
                && periodo.fechaFin().isBefore(LocalDate.now(ZoneId.of("America/Guayaquil")))
                ? EstadoPlanificacionAgregada.FINALIZADA : plan.getEstado();
    }

    private PlanificacionResponse mapBlock(PlanificacionJpaEntity item) {
        return new PlanificacionResponse(item.getId(), item.getPlanificacionId(), item.getNivel(), item.getPeriodoId(),
                item.getCarreraId(), item.getMateriaId(), item.getDocenteId(), item.getLaboratorioId(),
                item.getDiaSemana(), item.getHoraInicio(), item.getHoraFin(), item.getEstado().name(),
                item.getObservacion(), item.getCreadoPorPerfilId(), item.getCreadaEn(), item.getActualizadaEn(),
                item.getVersion());
    }
}
