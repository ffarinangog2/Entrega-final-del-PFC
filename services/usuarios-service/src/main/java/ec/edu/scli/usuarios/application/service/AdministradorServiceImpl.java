package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.presentation.dto.administrador.AdministradorRequest;
import ec.edu.scli.usuarios.presentation.dto.administrador.AdministradorResponse;
import ec.edu.scli.usuarios.domain.model.Administrador;
import ec.edu.scli.usuarios.domain.model.Perfil;
import ec.edu.scli.usuarios.domain.exception.BusinessRuleException;
import ec.edu.scli.usuarios.domain.exception.ConflictException;
import ec.edu.scli.usuarios.domain.exception.ResourceNotFoundException;
import ec.edu.scli.usuarios.domain.port.AdministradorRepositoryPort;
import ec.edu.scli.usuarios.domain.port.PerfilRepositoryPort;
import ec.edu.scli.usuarios.application.usecase.AdministradorService;
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
public class AdministradorServiceImpl
        implements AdministradorService {

    private final AdministradorRepositoryPort administradorRepository;

    private final PerfilRepositoryPort perfilRepository;

    private final AuditLogger auditLogger;

    public AdministradorServiceImpl(
            AdministradorRepositoryPort administradorRepository,
            PerfilRepositoryPort perfilRepository,
            AuditLogger auditLogger
    ) {
        this.administradorRepository = administradorRepository;
        this.perfilRepository = perfilRepository;
        this.auditLogger = auditLogger;
    }

    @Override
    @Transactional
    public AdministradorResponse crear(
            AdministradorRequest request
    ) {

        Perfil perfil = buscarPerfil(request.perfilId());

        if (!perfil.getActivo()) {

            throw new BusinessRuleException(
                    "No se puede crear un administrador "
                            + "con un perfil inactivo"
            );
        }

        if (administradorRepository.existsByPerfilId(
                request.perfilId()
        )) {

            throw new ConflictException(
                    "El perfil ya está registrado como administrador"
            );
        }

        if (administradorRepository
                .existsByCodigoAdministrador(
                        request.codigoAdministrador()
                )) {

            throw new ConflictException(
                    "Ya existe un administrador con el código: "
                            + request.codigoAdministrador()
            );
        }

        Administrador administrador = new Administrador();

        administrador.setPerfil(perfil);

        administrador.setCodigoAdministrador(
                request.codigoAdministrador()
        );

        administrador.setCargo(request.cargo());

        administrador.setPisoId(request.pisoId());

        if (request.activo() == null) {

            administrador.setActivo(true);

        } else {

            administrador.setActivo(request.activo());
        }

        Administrador administradorGuardado =
                administradorRepository.save(administrador);

        auditLogger.registrarEvento(
                "usuario_creado",
                usuarioActual(),
                ipCliente(),
                "tipo=administrador, id=" + administradorGuardado.getId()
        );

        return convertirAResponse(administradorGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdministradorResponse> listar(
            Pageable pageable
    ) {

        return PageableMapper.toSpringPage(administradorRepository.findAll(PageableMapper.toCriteria(pageable)).map(this::convertirAResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public AdministradorResponse obtenerPorId(UUID id) {

        Administrador administrador =
                buscarAdministrador(id);

        return convertirAResponse(administrador);
    }

    @Override
    @Transactional
    public AdministradorResponse actualizar(
            UUID id,
            AdministradorRequest request
    ) {

        Administrador administrador =
                buscarAdministrador(id);

        if (!administrador
                .getPerfil()
                .getId()
                .equals(request.perfilId())) {

            throw new BusinessRuleException(
                    "No se puede cambiar el perfil "
                            + "asociado al administrador"
            );
        }

        validarCodigoActualizacion(
                administrador,
                request.codigoAdministrador()
        );

        administrador.setCodigoAdministrador(
                request.codigoAdministrador()
        );

        administrador.setCargo(request.cargo());

        administrador.setPisoId(request.pisoId());

        Boolean activoAnterior = administrador.getActivo();

        if (request.activo() != null) {

            administrador.setActivo(request.activo());
        }

        Administrador administradorActualizado =
                administradorRepository.save(administrador);

        if (request.activo() != null
                && !request.activo().equals(activoAnterior)) {

            auditLogger.registrarEvento(
                    request.activo()
                            ? "usuario_reactivado"
                            : "usuario_desactivado",
                    usuarioActual(),
                    ipCliente(),
                    "tipo=administrador, id=" + id
            );
        }

        return convertirAResponse(administradorActualizado);
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

    private Administrador buscarAdministrador(UUID id) {

        return administradorRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "No existe un administrador con el id: "
                                        + id
                        )
                );
    }

    private void validarCodigoActualizacion(
            Administrador administrador,
            String nuevoCodigo
    ) {

        administradorRepository
                .findAll()
                .stream()
                .filter(encontrado ->
                        nuevoCodigo.equals(
                                encontrado.getCodigoAdministrador()
                        )
                )
                .filter(encontrado ->
                        !encontrado
                                .getId()
                                .equals(administrador.getId())
                )
                .findFirst()
                .ifPresent(encontrado -> {

                    throw new ConflictException(
                            "Ya existe otro administrador con el código: "
                                    + nuevoCodigo
                    );
                });
    }

    private AdministradorResponse convertirAResponse(
            Administrador administrador
    ) {

        return new AdministradorResponse(
                administrador.getId(),
                administrador.getPerfil().getId(),
                administrador.getCodigoAdministrador(),
                administrador.getCargo(),
                administrador.getPisoId(),
                administrador.getActivo(),
                administrador.getCreadoEn(),
                administrador.getActualizadoEn()
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