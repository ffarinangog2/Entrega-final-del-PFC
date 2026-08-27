package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.presentation.dto.estudiante.EstudianteRequest;
import ec.edu.scli.usuarios.presentation.dto.estudiante.EstudianteResponse;
import ec.edu.scli.usuarios.domain.model.Estudiante;
import ec.edu.scli.usuarios.domain.model.Perfil;
import ec.edu.scli.usuarios.domain.exception.BusinessRuleException;
import ec.edu.scli.usuarios.domain.exception.ConflictException;
import ec.edu.scli.usuarios.domain.exception.ResourceNotFoundException;
import ec.edu.scli.usuarios.domain.port.EstudianteRepositoryPort;
import ec.edu.scli.usuarios.domain.port.PerfilRepositoryPort;
import ec.edu.scli.usuarios.application.usecase.EstudianteService;
import ec.edu.scli.usuarios.infrastructure.audit.AuditLogger;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Service
public class EstudianteServiceImpl implements EstudianteService {

    private final EstudianteRepositoryPort estudianteRepository;
    private final PerfilRepositoryPort perfilRepository;
    private final AuditLogger auditLogger;

    public EstudianteServiceImpl(
            EstudianteRepositoryPort estudianteRepository,
            PerfilRepositoryPort perfilRepository,
            AuditLogger auditLogger
    ) {
        this.estudianteRepository = estudianteRepository;
        this.perfilRepository = perfilRepository;
        this.auditLogger = auditLogger;
    }

    @Override
    @Transactional
    public EstudianteResponse crear(EstudianteRequest request) {

        Perfil perfil = buscarPerfil(request.perfilId());

        if (!perfil.getActivo()) {
            throw new BusinessRuleException(
                    "No se puede crear un estudiante con un perfil inactivo"
            );
        }

        if (estudianteRepository.existsByPerfilId(request.perfilId())) {
            throw new ConflictException(
                    "El perfil ya está registrado como estudiante"
            );
        }

        if (estudianteRepository.existsByMatricula(request.matricula())) {
            throw new ConflictException(
                    "Ya existe un estudiante con la matrícula: "
                            + request.matricula()
            );
        }

        Estudiante estudiante = new Estudiante();

        estudiante.setPerfil(perfil);
        estudiante.setMatricula(request.matricula());
        estudiante.setCarreraId(request.carreraId());
        estudiante.setSemestre(request.semestre());

        if (request.activo() == null) {
            estudiante.setActivo(true);
        } else {
            estudiante.setActivo(request.activo());
        }

        Estudiante estudianteGuardado =
                estudianteRepository.save(estudiante);

        auditLogger.registrarEvento(
                "usuario_creado",
                usuarioActual(),
                ipCliente(),
                "tipo=estudiante, id=" + estudianteGuardado.getId()
        );

        return convertirAResponse(estudianteGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EstudianteResponse> listar(Pageable pageable) {

        return PageableMapper.toSpringPage(estudianteRepository.findAll(PageableMapper.toCriteria(pageable)).map(this::convertirAResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public EstudianteResponse obtenerPorId(UUID id) {

        Estudiante estudiante = buscarEstudiante(id);

        return convertirAResponse(estudiante);
    }

    @Override
    @Transactional
    public EstudianteResponse actualizar(
            UUID id,
            EstudianteRequest request
    ) {

        Estudiante estudiante = buscarEstudiante(id);

        if (!estudiante
                .getPerfil()
                .getId()
                .equals(request.perfilId())) {

            throw new BusinessRuleException(
                    "No se puede cambiar el perfil asociado al estudiante"
            );
        }

        validarMatriculaActualizacion(
                estudiante,
                request.matricula()
        );

        estudiante.setMatricula(request.matricula());
        estudiante.setCarreraId(request.carreraId());
        estudiante.setSemestre(request.semestre());

        Boolean activoAnterior = estudiante.getActivo();

        if (request.activo() != null) {
            estudiante.setActivo(request.activo());
        }

        Estudiante estudianteActualizado =
                estudianteRepository.save(estudiante);

        if (request.activo() != null
                && !request.activo().equals(activoAnterior)) {

            auditLogger.registrarEvento(
                    request.activo()
                            ? "usuario_reactivado"
                            : "usuario_desactivado",
                    usuarioActual(),
                    ipCliente(),
                    "tipo=estudiante, id=" + id
            );
        }

        return convertirAResponse(estudianteActualizado);
    }

    private Perfil buscarPerfil(UUID perfilId) {

        return perfilRepository
                .findById(perfilId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "No existe un perfil con el id: "
                                        + perfilId
                        )
                );
    }

    private Estudiante buscarEstudiante(UUID id) {

        return estudianteRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "No existe un estudiante con el id: "
                                        + id
                        )
                );
    }

    private void validarMatriculaActualizacion(
            Estudiante estudiante,
            String nuevaMatricula
    ) {

        estudianteRepository
                .findAll()
                .stream()
                .filter(encontrado ->
                        nuevaMatricula.equals(
                                encontrado.getMatricula()
                        )
                )
                .filter(encontrado ->
                        !encontrado
                                .getId()
                                .equals(estudiante.getId())
                )
                .findFirst()
                .ifPresent(encontrado -> {
                    throw new ConflictException(
                            "Ya existe otro estudiante con la matrícula: "
                                    + nuevaMatricula
                    );
                });
    }

    private EstudianteResponse convertirAResponse(
            Estudiante estudiante
    ) {

        return new EstudianteResponse(
                estudiante.getId(),
                estudiante.getPerfil().getId(),
                estudiante.getMatricula(),
                estudiante.getCarreraId(),
                estudiante.getSemestre(),
                estudiante.getActivo(),
                estudiante.getCreadoEn(),
                estudiante.getActualizadoEn()
        );
    }

    private String usuarioActual() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {

            return "sistema";
        }

        return authentication.getName();
    }

    private String ipCliente() {

        var attributes = RequestContextHolder.getRequestAttributes();

        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {

            return "desconocida";
        }

        HttpServletRequest request = servletAttributes.getRequest();

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {

            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}