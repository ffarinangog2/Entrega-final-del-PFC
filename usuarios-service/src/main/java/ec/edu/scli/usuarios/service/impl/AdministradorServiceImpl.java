package ec.edu.scli.usuarios.service.impl;

import ec.edu.scli.usuarios.dto.administrador.AdministradorRequest;
import ec.edu.scli.usuarios.dto.administrador.AdministradorResponse;
import ec.edu.scli.usuarios.entity.Administrador;
import ec.edu.scli.usuarios.entity.Perfil;
import ec.edu.scli.usuarios.exception.BusinessRuleException;
import ec.edu.scli.usuarios.exception.ConflictException;
import ec.edu.scli.usuarios.exception.ResourceNotFoundException;
import ec.edu.scli.usuarios.repository.AdministradorRepository;
import ec.edu.scli.usuarios.repository.PerfilRepository;
import ec.edu.scli.usuarios.service.AdministradorService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdministradorServiceImpl
        implements AdministradorService {

    private final AdministradorRepository administradorRepository;

    private final PerfilRepository perfilRepository;

    public AdministradorServiceImpl(
            AdministradorRepository administradorRepository,
            PerfilRepository perfilRepository
    ) {
        this.administradorRepository = administradorRepository;
        this.perfilRepository = perfilRepository;
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

        return convertirAResponse(administradorGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdministradorResponse> listar(
            Pageable pageable
    ) {

        return administradorRepository
                .findAll(pageable)
                .map(this::convertirAResponse);
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

        if (request.activo() != null) {

            administrador.setActivo(request.activo());
        }

        Administrador administradorActualizado =
                administradorRepository.save(administrador);

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
}