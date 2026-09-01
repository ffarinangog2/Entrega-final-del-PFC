package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.domain.model.ActorAutenticado;
import ec.edu.scli.reservas.domain.model.EstadoPlanificacion;
import ec.edu.scli.reservas.domain.port.out.ActorActualPort;
import ec.edu.scli.reservas.domain.port.out.ContextoInstitucionalPort;
import ec.edu.scli.reservas.domain.port.out.DocenteInstitucionalPort;
import ec.edu.scli.reservas.infrastructure.persistence.entity.PlanificacionJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.repository.PlanificacionJpaRepository;
import ec.edu.scli.reservas.presentation.dto.request.GuardarPlanificacionRequest;
import ec.edu.scli.reservas.presentation.dto.request.ProponerPlanificacionRequest;
import ec.edu.scli.reservas.presentation.dto.response.PlanificacionResponse;
import ec.edu.scli.reservas.presentation.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PlanificacionService {
    private final PlanificacionJpaRepository repository;
    private final ActorActualPort actores;
    private final ContextoInstitucionalPort contextos;
    private final PoliticaAmbitoLaboratorio ambitoLaboratorio;
    private final AcademicoLaboratoriosClient academico;
    private final NotificacionService notificaciones;
    private final DocenteInstitucionalPort docentes;

    public PlanificacionService(PlanificacionJpaRepository repository, ActorActualPort actores,
            ContextoInstitucionalPort contextos, PoliticaAmbitoLaboratorio ambitoLaboratorio,
            AcademicoLaboratoriosClient academico, NotificacionService notificaciones,
            DocenteInstitucionalPort docentes) {
        this.repository = repository;
        this.actores = actores;
        this.contextos = contextos;
        this.ambitoLaboratorio = ambitoLaboratorio;
        this.academico = academico;
        this.notificaciones = notificaciones;
        this.docentes = docentes;
    }

    @Transactional
    public PlanificacionResponse crear(GuardarPlanificacionRequest request) {
        ActorAutenticado actor = coordinador();
        validarCarrera(actor, request.carreraId());
        validarDatos(request);
        PlanificacionJpaEntity entity = new PlanificacionJpaEntity();
        copiar(request, entity);
        entity.setEstado(EstadoPlanificacion.BORRADOR);
        entity.setCreadoPorPerfilId(actor.perfilId());
        Instant now = Instant.now();
        entity.setCreadaEn(now);
        entity.setActualizadaEn(now);
        return map(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<PlanificacionResponse> listar() {
        ActorAutenticado actor = actores.obtener();
        if (actor.tiene("ROLE_ADMINISTRADOR")) return repository.findAll().stream().map(this::map).toList();
        if (actor.tiene("ROLE_COORDINADOR")) {
            UUID carrera = carreraCoordinada(actor);
            return repository.findByCarreraId(carrera).stream().map(this::map).toList();
        }
        if (actor.tiene("ROLE_ADMINISTRADOR_PISO")) {
            UUID piso = ambitoLaboratorio.pisoGestionado();
            return repository.findAll().stream()
                    .filter(p -> piso.equals(ambitoLaboratorio.obtenerPiso(p.getLaboratorioId())))
                    .map(this::map).toList();
        }
        throw new AccessDeniedException("No puede consultar planificaciones");
    }

    @Transactional(readOnly = true)
    public PlanificacionResponse buscar(UUID id) {
        PlanificacionJpaEntity entity = obtener(id);
        validarLectura(entity);
        return map(entity);
    }

    @Transactional
    public PlanificacionResponse editar(UUID id, GuardarPlanificacionRequest request) {
        PlanificacionJpaEntity entity = obtener(id);
        ActorAutenticado actor = coordinador();
        validarCarrera(actor, entity.getCarreraId());
        validarCarrera(actor, request.carreraId());
        if (entity.getEstado() != EstadoPlanificacion.BORRADOR
                && entity.getEstado() != EstadoPlanificacion.PROPUESTA_CAMBIO) {
            throw new IllegalStateException("Solo puede editarse un borrador o una propuesta de cambio");
        }
        validarDatos(request);
        copiar(request, entity);
        entity.setActualizadaEn(Instant.now());
        return map(repository.save(entity));
    }

    @Transactional
    public PlanificacionResponse enviar(UUID id) {
        PlanificacionJpaEntity entity = autorizadaCoordinador(id);
        transicionar(entity, EstadoPlanificacion.BORRADOR, EstadoPlanificacion.ENVIADA);
        return guardar(entity);
    }

    @Transactional
    public PlanificacionResponse reenviar(UUID id) {
        PlanificacionJpaEntity entity = autorizadaCoordinador(id);
        transicionar(entity, EstadoPlanificacion.PROPUESTA_CAMBIO, EstadoPlanificacion.ENVIADA);
        return guardar(entity);
    }

    @Transactional
    public PlanificacionResponse aceptar(UUID id) {
        PlanificacionJpaEntity entity = autorizadaAdministrador(id);
        transicionar(entity, EstadoPlanificacion.ENVIADA, EstadoPlanificacion.CONFIRMADA);
        validarSinConflicto(entity);
        PlanificacionResponse response = guardar(entity);
        notificar(entity, "Planificacion aceptada", "La planificacion semestral fue confirmada", "ACEPTADA");
        return response;
    }

    @Transactional
    public PlanificacionResponse rechazar(UUID id, String observacion) {
        PlanificacionJpaEntity entity = autorizadaAdministrador(id);
        transicionar(entity, EstadoPlanificacion.ENVIADA, EstadoPlanificacion.RECHAZADA);
        entity.setObservacion(observacion);
        PlanificacionResponse response = guardar(entity);
        notificar(entity, "Planificacion rechazada", "La planificacion semestral fue rechazada", "RECHAZADA");
        return response;
    }

    @Transactional
    public PlanificacionResponse proponer(UUID id, ProponerPlanificacionRequest request) {
        PlanificacionJpaEntity entity = autorizadaAdministrador(id);
        transicionar(entity, EstadoPlanificacion.ENVIADA, EstadoPlanificacion.PROPUESTA_CAMBIO);
        if (request.laboratorioId() != null) {
            ambitoLaboratorio.validarGestion(request.laboratorioId());
            entity.setLaboratorioId(request.laboratorioId());
        }
        LocalTime inicio = request.horaInicio() == null ? entity.getHoraInicio() : request.horaInicio();
        LocalTime fin = request.horaFin() == null ? entity.getHoraFin() : request.horaFin();
        validarHoras(inicio, fin);
        entity.setHoraInicio(inicio);
        entity.setHoraFin(fin);
        entity.setObservacion(request.observacion());
        PlanificacionResponse response = guardar(entity);
        notificar(entity, "Propuesta de planificacion", "Existe una alternativa para la planificacion", "PROPUESTA_CAMBIO");
        return response;
    }

    @Transactional
    public PlanificacionResponse aceptarPropuesta(UUID id) {
        PlanificacionJpaEntity entity = autorizadaCoordinador(id);
        transicionar(entity, EstadoPlanificacion.PROPUESTA_CAMBIO, EstadoPlanificacion.CONFIRMADA);
        validarSinConflicto(entity);
        return guardar(entity);
    }

    @Transactional
    public PlanificacionResponse cancelar(UUID id) {
        PlanificacionJpaEntity entity = obtener(id);
        ActorAutenticado actor = actores.obtener();
        if (!actor.tiene("ROLE_ADMINISTRADOR")) validarCarrera(coordinador(), entity.getCarreraId());
        if (entity.getEstado() == EstadoPlanificacion.CONFIRMADA
                || entity.getEstado() == EstadoPlanificacion.RECHAZADA
                || entity.getEstado() == EstadoPlanificacion.CANCELADA) {
            throw new IllegalStateException("La planificacion no puede cancelarse en su estado actual");
        }
        entity.setEstado(EstadoPlanificacion.CANCELADA);
        return guardar(entity);
    }

    private PlanificacionJpaEntity autorizadaCoordinador(UUID id) {
        PlanificacionJpaEntity entity = obtener(id);
        validarCarrera(coordinador(), entity.getCarreraId());
        return entity;
    }

    private PlanificacionJpaEntity autorizadaAdministrador(UUID id) {
        PlanificacionJpaEntity entity = obtener(id);
        ActorAutenticado actor = actores.obtener();
        if (!actor.tiene("ROLE_ADMINISTRADOR")) ambitoLaboratorio.validarGestion(entity.getLaboratorioId());
        return entity;
    }

    private void validarLectura(PlanificacionJpaEntity entity) {
        ActorAutenticado actor = actores.obtener();
        if (actor.tiene("ROLE_ADMINISTRADOR")) return;
        if (actor.tiene("ROLE_COORDINADOR")) {
            validarCarrera(actor, entity.getCarreraId());
            return;
        }
        if (actor.tiene("ROLE_ADMINISTRADOR_PISO")) {
            ambitoLaboratorio.validarGestion(entity.getLaboratorioId());
            return;
        }
        throw new AccessDeniedException("No puede consultar esta planificacion");
    }

    private ActorAutenticado coordinador() {
        ActorAutenticado actor = actores.obtener();
        if (!actor.tiene("ROLE_COORDINADOR")) throw new AccessDeniedException("Solo un coordinador puede operar este flujo");
        return actor;
    }

    private UUID carreraCoordinada(ActorAutenticado actor) {
        var contexto = contextos.obtenerPorPerfilId(actor.perfilId());
        if (contexto == null || !contexto.perfilExiste() || !contexto.perfilActivo()
                || contexto.carreraIds().size() != 1) {
            throw new AccessDeniedException("El coordinador no posee una carrera institucional unica y activa");
        }
        return contexto.carreraIds().getFirst();
    }

    private void validarCarrera(ActorAutenticado actor, UUID carreraId) {
        if (!carreraCoordinada(actor).equals(carreraId)) {
            throw new AccessDeniedException("La carrera no pertenece al coordinador");
        }
    }

    private void validarDatos(GuardarPlanificacionRequest request) {
        if (!academico.existePeriodoLectivo(request.periodoId())) throw new IllegalArgumentException("El periodo no existe");
        var materia = academico.obtenerContextoMateria(request.materiaId());
        if (materia == null || !materia.existe() || !materia.activo()) throw new IllegalArgumentException("La materia no existe o esta inactiva");
        if (!request.carreraId().equals(materia.carreraId())) throw new IllegalArgumentException("La materia no pertenece a la carrera");
        if (request.docenteId() != null) {
            var docente = docentes.obtenerPorDocenteId(request.docenteId());
            if (docente == null || !docente.activo()) throw new IllegalArgumentException("El docente no existe o esta inactivo");
        }
        var laboratorio = academico.obtenerLaboratorio(request.laboratorioId());
        if (laboratorio == null || !laboratorio.existe()) throw new IllegalArgumentException("El laboratorio no existe");
        validarHoras(request.horaInicio(), request.horaFin());
        normalizarDia(request.diaSemana());
    }

    private void validarHoras(LocalTime inicio, LocalTime fin) {
        if (inicio == null || fin == null || !inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin");
        }
    }

    private String normalizarDia(String dia) {
        String value = dia == null ? "" : dia.strip().toUpperCase(Locale.ROOT);
        if (!List.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO").contains(value)) {
            throw new IllegalArgumentException("Dia de semana invalido");
        }
        return value;
    }

    private void copiar(GuardarPlanificacionRequest request, PlanificacionJpaEntity entity) {
        entity.setPeriodoId(request.periodoId());
        entity.setCarreraId(request.carreraId());
        entity.setMateriaId(request.materiaId());
        entity.setDocenteId(request.docenteId());
        entity.setLaboratorioId(request.laboratorioId());
        entity.setDiaSemana(normalizarDia(request.diaSemana()));
        entity.setHoraInicio(request.horaInicio());
        entity.setHoraFin(request.horaFin());
        entity.setObservacion(request.observacion());
    }

    private void validarSinConflicto(PlanificacionJpaEntity entity) {
        if (!repository.bloquearConflictos(entity.getLaboratorioId(), entity.getDiaSemana(),
                entity.getHoraInicio(), entity.getHoraFin(), EstadoPlanificacion.CONFIRMADA,
                entity.getId()).isEmpty()) {
            throw new IllegalStateException("La franja ya esta ocupada por otra planificacion confirmada");
        }
    }

    private void transicionar(PlanificacionJpaEntity entity, EstadoPlanificacion origen, EstadoPlanificacion destino) {
        if (entity.getEstado() != origen) throw new IllegalStateException("Transicion de planificacion invalida");
        entity.setEstado(destino);
    }

    private PlanificacionResponse guardar(PlanificacionJpaEntity entity) {
        entity.setActualizadaEn(Instant.now());
        return map(repository.saveAndFlush(entity));
    }

    private void notificar(PlanificacionJpaEntity entity, String titulo, String cuerpo, String evento) {
        notificaciones.notificarPerfil(entity.getCreadoPorPerfilId(), titulo, cuerpo,
                java.util.Map.of("tipo", "PLANIFICACION", "evento", evento, "planificacionId", entity.getId().toString()));
    }

    private PlanificacionJpaEntity obtener(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Planificacion no encontrada"));
    }

    private PlanificacionResponse map(PlanificacionJpaEntity p) {
        return new PlanificacionResponse(p.getId(), p.getPeriodoId(), p.getCarreraId(), p.getMateriaId(),
                p.getDocenteId(), p.getLaboratorioId(), p.getDiaSemana(), p.getHoraInicio(), p.getHoraFin(),
                p.getEstado().name(), p.getObservacion(), p.getCreadoPorPerfilId(), p.getCreadaEn(),
                p.getActualizadaEn(), p.getVersion());
    }
}
