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
    private final SecureRandom random = new SecureRandom();

    public AsistenciaService(SesionAsistenciaJpaRepository sesiones, RegistroAsistenciaJpaRepository registros,
            ReservaService reservas, EstudianteInstitucionalPort estudiantes,
            ReservaRepositoryPort reservaRepository,
            SolicitudReservaRepositoryPort solicitudRepository,
            AcademicoLaboratoriosClient academico,
            @Value("${app.asistencia.window-minutes:15}") long minutos) {
        this.sesiones = sesiones;
        this.registros = registros;
        this.reservas = reservas;
        this.minutos = minutos;
        this.estudiantes = estudiantes;
        this.reservaRepository = reservaRepository;
        this.solicitudRepository = solicitudRepository;
        this.academico = academico;
    }

    @Transactional
    public SesionAsistenciaResponse abrir(AbrirSesionAsistenciaRequest request, UUID actor) {
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
        UUID carreraId = estudiantes.resolverCarreraActiva(perfilEstudiante);
        Instant now = Instant.now();
        return sesiones.findByEstado(EstadoSesionAsistencia.ABIERTA).stream()
                .filter(sesion -> !now.isAfter(sesion.getExpiraEn()))
                .filter(sesion -> perteneceCarrera(sesion, carreraId))
                .map(sesion -> response(sesion, null))
                .toList();
    }

    @Transactional
    public RegistroAsistenciaResponse registrarPropia(UUID sesionId, UUID perfilEstudiante) {
        UUID carreraId = estudiantes.resolverCarreraActiva(perfilEstudiante);
        SesionAsistenciaJpaEntity sesion = obtener(sesionId);
        if (!perteneceCarrera(sesion, carreraId)) {
            throw new AccessDeniedException("La sesión no corresponde a la carrera del estudiante");
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
        return new SesionAsistenciaResponse(e.getId(), e.getReservaId(), e.getAbiertaEn(), e.getExpiraEn(), e.getEstado().name(), token);
    }
    private RegistroAsistenciaResponse map(RegistroAsistenciaJpaEntity e) {
        return new RegistroAsistenciaResponse(e.getId(), e.getSesionId(), e.getEstudianteId(), e.getRegistradaEn(), e.getEstado());
    }
    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no esta disponible", exception);
        }
    }
}
