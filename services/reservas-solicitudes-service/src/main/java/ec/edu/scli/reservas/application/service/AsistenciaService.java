package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.domain.model.EstadoSesionAsistencia;
import ec.edu.scli.reservas.infrastructure.persistence.entity.RegistroAsistenciaJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.SesionAsistenciaJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.repository.RegistroAsistenciaJpaRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.SesionAsistenciaJpaRepository;
import ec.edu.scli.reservas.presentation.dto.request.AbrirSesionAsistenciaRequest;
import ec.edu.scli.reservas.presentation.dto.request.RegistrarAsistenciaRequest;
import ec.edu.scli.reservas.presentation.dto.response.RegistroAsistenciaResponse;
import ec.edu.scli.reservas.presentation.dto.response.SesionAsistenciaResponse;
import ec.edu.scli.reservas.presentation.dto.response.PlanificacionResponse;
import ec.edu.scli.reservas.presentation.exception.ResourceNotFoundException;
import ec.edu.scli.reservas.domain.port.out.EstudianteInstitucionalPort;
import ec.edu.scli.reservas.domain.port.out.ReservaRepositoryPort;
import ec.edu.scli.reservas.domain.port.out.SolicitudReservaRepositoryPort;
import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AsistenciaService {
    private final SesionAsistenciaJpaRepository sesiones;
    private final RegistroAsistenciaJpaRepository registros;
    private final ReservaService reservas;
    private final long minutos;
    private final EstudianteInstitucionalPort estudiantes;
    private final ReservaRepositoryPort reservaRepository;
    private final SolicitudReservaRepositoryPort solicitudRepository;
    private final AcademicoLaboratoriosClient academico;
    private final ec.edu.scli.reservas.infrastructure.persistence.repository.PlanificacionJpaRepository bloques;
    private final ec.edu.scli.reservas.infrastructure.persistence.repository.PlanificacionAgregadaJpaRepository planes;
    private final ec.edu.scli.reservas.client.UsuariosClient usuarios;
    private final SecureRandom random = new SecureRandom();

    public AsistenciaService(SesionAsistenciaJpaRepository sesiones, RegistroAsistenciaJpaRepository registros,
            ReservaService reservas, EstudianteInstitucionalPort estudiantes,
            ReservaRepositoryPort reservaRepository,
            SolicitudReservaRepositoryPort solicitudRepository,
            AcademicoLaboratoriosClient academico,
            ec.edu.scli.reservas.infrastructure.persistence.repository.PlanificacionJpaRepository bloques,
            ec.edu.scli.reservas.infrastructure.persistence.repository.PlanificacionAgregadaJpaRepository planes,
            ec.edu.scli.reservas.client.UsuariosClient usuarios,
            @Value("${app.asistencia.window-minutes:15}") long minutos) {
        this.sesiones = sesiones;
        this.registros = registros;
        this.reservas = reservas;
        this.minutos = minutos;
        this.estudiantes = estudiantes;
        this.reservaRepository = reservaRepository;
        this.solicitudRepository = solicitudRepository;
        this.academico = academico;
        this.bloques = bloques;
        this.planes = planes;
        this.usuarios = usuarios;
    }

    @Transactional
    public SesionAsistenciaResponse abrir(AbrirSesionAsistenciaRequest request, UUID actor) {
        if (request.bloqueId() != null) return abrirBloque(request.bloqueId(), actor);
        if (request.reservaId() == null) throw new IllegalArgumentException("Debe seleccionar una clase planificada");
        var reserva = reservas.buscarPorId(request.reservaId());
        if (!actor.equals(reserva.responsableId())) throw new AccessDeniedException("La reserva no pertenece al docente");
        sesiones.findFirstByReservaIdAndEstado(request.reservaId(), EstadoSesionAsistencia.ABIERTA)
                .ifPresent(s -> { throw new IllegalStateException("La reserva ya tiene una sesion abierta"); });
        Instant now = Instant.now();
        String token = tokenSeguro();
        SesionAsistenciaJpaEntity entity = new SesionAsistenciaJpaEntity();
        entity.setReservaId(request.reservaId());
        entity.setDocenteId(actor);
        entity.setAbiertaEn(now);
        entity.setExpiraEn(now.plusSeconds(minutos * 60));
        entity.setTokenHash(hash(token));
        return response(sesiones.save(entity), token);
    }

    private SesionAsistenciaResponse abrirBloque(UUID bloqueId, UUID actor) {
        var bloque = bloques.findById(bloqueId).orElseThrow(() -> new ResourceNotFoundException("Bloque de planificación no encontrado"));
        var plan = planes.findById(bloque.getPlanificacionId()).orElseThrow(() -> new ResourceNotFoundException("Planificación no encontrada"));
        if (plan.getEstado() != ec.edu.scli.reservas.domain.model.EstadoPlanificacionAgregada.APROBADA)
            throw new IllegalStateException("La planificación todavía no está aprobada");
        var docente=usuarios.obtenerDocentePorId(bloque.getDocenteId());
        if (docente==null || !actor.equals(docente.perfilId())) throw new AccessDeniedException("El bloque no pertenece al docente autenticado");
        ZoneId zona = ZoneId.of("America/Guayaquil");
        LocalDate fecha = LocalDate.now(zona);
        if (!dia(fecha).equalsIgnoreCase(bloque.getDiaSemana())) throw new IllegalStateException("La clase no corresponde al día actual");
        LocalTime hora = LocalTime.now(zona);
        if (hora.isBefore(bloque.getHoraInicio()) || hora.isAfter(bloque.getHoraFin())) throw new IllegalStateException("La clase no está dentro de su horario");
        sesiones.findFirstByBloquePlanificacionIdAndFechaClaseAndEstado(bloqueId, fecha, EstadoSesionAsistencia.ABIERTA)
                .ifPresent(s -> { throw new IllegalStateException("La clase ya tiene una sesión abierta"); });
        Instant now=Instant.now(); String token=tokenSeguro(); var entity=new SesionAsistenciaJpaEntity();
        entity.setBloquePlanificacionId(bloqueId); entity.setFechaClase(fecha); entity.setDocenteId(actor);
        entity.setAbiertaEn(now); entity.setExpiraEn(now.plusSeconds(minutos*60)); entity.setTokenHash(hash(token));
        return response(sesiones.save(entity),token);
    }

    @Transactional
    public RegistroAsistenciaResponse registrar(UUID sesionId, RegistrarAsistenciaRequest request, UUID perfilEstudiante) {
        UUID estudiante = estudiantes.resolverEstudianteActivo(perfilEstudiante);
        SesionAsistenciaJpaEntity sesion = obtener(sesionId);
        Instant now = Instant.now();
        if (!MessageDigest.isEqual(hash(request.token()).getBytes(StandardCharsets.US_ASCII),
                sesion.getTokenHash().getBytes(StandardCharsets.US_ASCII))) throw new AccessDeniedException("Token de asistencia invalido");
        if (now.isAfter(sesion.getExpiraEn())) {
            sesion.setEstado(EstadoSesionAsistencia.VENCIDA);
            sesiones.save(sesion);
            throw new IllegalStateException("La sesion de asistencia esta vencida");
        }
        if (sesion.getEstado() != EstadoSesionAsistencia.ABIERTA) throw new IllegalStateException("La sesion no esta abierta");
        if (registros.existsBySesionIdAndEstudianteId(sesionId, estudiante)) throw new IllegalStateException("La asistencia ya fue registrada");
        RegistroAsistenciaJpaEntity registro = new RegistroAsistenciaJpaEntity();
        registro.setSesionId(sesionId);
        registro.setEstudianteId(estudiante);
        registro.setRegistradaEn(now);
        return map(registros.save(registro));
    }

    @Transactional(readOnly = true)
    public List<SesionAsistenciaResponse> sesionesAbiertas(UUID perfilEstudiante) {
        var contexto = estudiantes.resolverContextoActivo(perfilEstudiante);
        Instant now = Instant.now();
        return sesiones.findByEstado(EstadoSesionAsistencia.ABIERTA).stream()
                .filter(sesion -> !now.isAfter(sesion.getExpiraEn()))
                .filter(sesion -> perteneceContexto(sesion, contexto))
                .map(sesion -> response(sesion, null))
                .toList();
    }

    @Transactional
    public RegistroAsistenciaResponse registrarPropia(UUID sesionId, UUID perfilEstudiante) {
        var contexto = estudiantes.resolverContextoActivo(perfilEstudiante);
        SesionAsistenciaJpaEntity sesion = obtener(sesionId);
        if (!perteneceContexto(sesion, contexto)) {
            throw new AccessDeniedException("La sesión no corresponde al contexto académico del estudiante");
        }
        return crearRegistro(sesion, estudiantes.resolverEstudianteActivo(perfilEstudiante));
    }

    @Transactional
    public void cerrar(UUID id, UUID actor) {
        SesionAsistenciaJpaEntity sesion = sesionPropia(id, actor);
        if (sesion.getEstado() == EstadoSesionAsistencia.CERRADA) return;
        if (sesion.getEstado() == EstadoSesionAsistencia.VENCIDA) throw new IllegalStateException("La sesion ya esta vencida");
        sesion.setEstado(EstadoSesionAsistencia.CERRADA);
        sesion.setCerradaEn(Instant.now());
        sesiones.save(sesion);
    }

    @Transactional(readOnly = true)
    public SesionAsistenciaResponse consultar(UUID id, UUID actor) { return response(sesionPropia(id, actor), null); }
    @Transactional(readOnly = true)
    public List<RegistroAsistenciaResponse> listar(UUID id, UUID actor) {
        sesionPropia(id, actor);
        return registros.findBySesionId(id).stream().map(this::map).toList();
    }
    @Transactional(readOnly = true)
    public List<RegistroAsistenciaResponse> historial(UUID perfilEstudiante) {
        UUID estudiante = estudiantes.resolverEstudianteActivo(perfilEstudiante);
        return registros.findByEstudianteId(estudiante).stream().map(this::map).toList();
    }
    @Transactional(readOnly = true)
    public List<RegistroAsistenciaResponse> historial(UUID perfilEstudiante, UUID periodoId) {
        if (periodoId == null) return historial(perfilEstudiante);
        var contexto = estudiantes.resolverContexto(perfilEstudiante, periodoId);
        UUID estudiante = estudiantes.resolverEstudianteActivo(perfilEstudiante);
        return registros.findByEstudianteId(estudiante).stream()
                .filter(registro -> sesiones.findById(registro.getSesionId()).map(sesion -> perteneceContexto(sesion, contexto)).orElse(false))
                .map(this::map).toList();
    }
    @Transactional(readOnly = true)
    public List<PlanificacionResponse> horario(UUID perfilEstudiante, UUID periodoId) {
        var contexto = periodoId == null ? estudiantes.resolverContextoActivo(perfilEstudiante)
                : estudiantes.resolverContexto(perfilEstudiante, periodoId);
        var plan = planes.findByCarreraIdAndPeriodoId(contexto.carreraId(), contexto.periodoId()).orElse(null);
        if (plan == null || plan.getEstado() != ec.edu.scli.reservas.domain.model.EstadoPlanificacionAgregada.APROBADA) return List.of();
        return bloques.findByPlanificacionId(plan.getId()).stream().filter(b -> contexto.nivel().equals(b.getNivel()))
                .map(b -> new PlanificacionResponse(b.getId(),b.getPlanificacionId(),b.getNivel(),b.getPeriodoId(),b.getCarreraId(),
                        b.getMateriaId(),b.getDocenteId(),b.getLaboratorioId(),b.getDiaSemana(),b.getHoraInicio(),b.getHoraFin(),
                        b.getEstado().name(),b.getObservacion(),b.getCreadoPorPerfilId(),b.getCreadaEn(),b.getActualizadaEn(),b.getVersion())).toList();
    }
    @Transactional(readOnly = true)
    public List<PlanificacionResponse> clasesDocenteHoy(UUID docenteId) {
        var docente=usuarios.obtenerDocentePorPerfil(docenteId);
        if(docente==null||!docente.activo()) throw new AccessDeniedException("No existe docente activo para el perfil autenticado");
        return bloques.findByDocenteIdAndDiaSemana(docente.docenteId(),dia(LocalDate.now(ZoneId.of("America/Guayaquil")))).stream()
                .filter(b -> b.getPlanificacionId()!=null).filter(b -> planes.findById(b.getPlanificacionId())
                        .map(p -> p.getEstado()==ec.edu.scli.reservas.domain.model.EstadoPlanificacionAgregada.APROBADA).orElse(false))
                .map(b -> new PlanificacionResponse(b.getId(),b.getPlanificacionId(),b.getNivel(),b.getPeriodoId(),b.getCarreraId(),b.getMateriaId(),b.getDocenteId(),b.getLaboratorioId(),b.getDiaSemana(),b.getHoraInicio(),b.getHoraFin(),b.getEstado().name(),b.getObservacion(),b.getCreadoPorPerfilId(),b.getCreadaEn(),b.getActualizadaEn(),b.getVersion())).toList();
    }

    private SesionAsistenciaJpaEntity sesionPropia(UUID id, UUID actor) {
        return sesiones.findByIdAndDocenteId(id, actor)
                .orElseThrow(() -> new AccessDeniedException("La sesion no pertenece al docente"));
    }
    private SesionAsistenciaJpaEntity obtener(UUID id) {
        return sesiones.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sesion de asistencia no encontrada"));
    }
    private boolean perteneceCarrera(SesionAsistenciaJpaEntity sesion, UUID carreraId) {
        var reserva = reservaRepository.buscarPorId(sesion.getReservaId()).orElse(null);
        if (reserva == null) return false;
        var solicitud = solicitudRepository.buscarPorId(reserva.getSolicitudId()).orElse(null);
        if (solicitud == null) return false;
        var materia = academico.obtenerContextoMateria(solicitud.getMateriaId());
        return materia != null && materia.existe() && carreraId.equals(materia.carreraId());
    }
    private boolean perteneceContexto(SesionAsistenciaJpaEntity sesion, EstudianteInstitucionalPort.Contexto contexto) {
        if (sesion.getBloquePlanificacionId() != null) {
            var bloque=bloques.findById(sesion.getBloquePlanificacionId()).orElse(null);
            if (bloque==null || !contexto.nivel().equals(bloque.getNivel())) return false;
            var plan=planes.findById(bloque.getPlanificacionId()).orElse(null);
            return plan!=null && plan.getEstado()==ec.edu.scli.reservas.domain.model.EstadoPlanificacionAgregada.APROBADA
                    && contexto.carreraId().equals(plan.getCarreraId()) && contexto.periodoId().equals(plan.getPeriodoId());
        }
        return perteneceCarrera(sesion, contexto.carreraId());
    }
    private RegistroAsistenciaResponse crearRegistro(SesionAsistenciaJpaEntity sesion, UUID estudiante) {
        Instant now = Instant.now();
        if (now.isAfter(sesion.getExpiraEn()) || sesion.getEstado() != EstadoSesionAsistencia.ABIERTA) {
            throw new IllegalStateException("La sesión de asistencia no está abierta");
        }
        if (registros.existsBySesionIdAndEstudianteId(sesion.getId(), estudiante)) {
            throw new IllegalStateException("La asistencia ya fue registrada");
        }
        RegistroAsistenciaJpaEntity registro = new RegistroAsistenciaJpaEntity();
        registro.setSesionId(sesion.getId());
        registro.setEstudianteId(estudiante);
        registro.setRegistradaEn(now);
        return map(registros.save(registro));
    }
    private String tokenSeguro() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private SesionAsistenciaResponse response(SesionAsistenciaJpaEntity e, String token) {
        return new SesionAsistenciaResponse(e.getId(), e.getReservaId(), e.getBloquePlanificacionId(), e.getFechaClase(), e.getAbiertaEn(), e.getExpiraEn(), e.getEstado().name(), token);
    }
    private RegistroAsistenciaResponse map(RegistroAsistenciaJpaEntity e) {
        UUID bloqueId = sesiones.findById(e.getSesionId()).map(SesionAsistenciaJpaEntity::getBloquePlanificacionId).orElse(null);
        return new RegistroAsistenciaResponse(e.getId(), e.getSesionId(), e.getEstudianteId(), bloqueId, e.getRegistradaEn(), e.getEstado());
    }
    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no esta disponible", exception);
        }
    }
    private String dia(LocalDate fecha) {
        return switch (fecha.getDayOfWeek()) { case MONDAY -> "LUNES"; case TUESDAY -> "MARTES";
            case WEDNESDAY -> "MIERCOLES"; case THURSDAY -> "JUEVES"; case FRIDAY -> "VIERNES";
            case SATURDAY -> "SABADO"; case SUNDAY -> "DOMINGO"; };
    }
}
