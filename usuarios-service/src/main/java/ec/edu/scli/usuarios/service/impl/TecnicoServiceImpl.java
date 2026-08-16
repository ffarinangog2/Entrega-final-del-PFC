package ec.edu.scli.usuarios.service.impl;

import ec.edu.scli.usuarios.dto.tecnico.TecnicoRequest;
import ec.edu.scli.usuarios.dto.tecnico.TecnicoResponse;
import ec.edu.scli.usuarios.entity.Perfil;
import ec.edu.scli.usuarios.entity.Tecnico;
import ec.edu.scli.usuarios.exception.BusinessRuleException;
import ec.edu.scli.usuarios.exception.ConflictException;
import ec.edu.scli.usuarios.exception.ResourceNotFoundException;
import ec.edu.scli.usuarios.repository.PerfilRepository;
import ec.edu.scli.usuarios.repository.TecnicoRepository;
import ec.edu.scli.usuarios.service.TecnicoService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TecnicoServiceImpl implements TecnicoService {

    private final TecnicoRepository tecnicoRepository;
    private final PerfilRepository perfilRepository;

    public TecnicoServiceImpl(
            TecnicoRepository tecnicoRepository,
            PerfilRepository perfilRepository
    ) {
        this.tecnicoRepository = tecnicoRepository;
        this.perfilRepository = perfilRepository;
    }

    @Override
    @Transactional
    public TecnicoResponse crear(TecnicoRequest request) {

        Perfil perfil = buscarPerfil(request.perfilId());

        if (!perfil.getActivo()) {
            throw new BusinessRuleException(
                    "No se puede crear un técnico con un perfil inactivo"
            );
        }

        if (tecnicoRepository.existsByPerfilId(request.perfilId())) {
            throw new ConflictException(
                    "El perfil ya está registrado como técnico"
            );
        }

        if (tecnicoRepository.existsByCodigoTecnico(
                request.codigoTecnico()
        )) {
            throw new ConflictException(
                    "Ya existe un técnico con el código: "
                            + request.codigoTecnico()
            );
        }

        Tecnico tecnico = new Tecnico();

        tecnico.setPerfil(perfil);
        tecnico.setCodigoTecnico(request.codigoTecnico());
        tecnico.setEspecialidad(request.especialidad());

        if (request.activo() == null) {
            tecnico.setActivo(true);
        } else {
            tecnico.setActivo(request.activo());
        }

        Tecnico tecnicoGuardado =
                tecnicoRepository.save(tecnico);

        return convertirAResponse(tecnicoGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TecnicoResponse> listar(Pageable pageable) {

        return tecnicoRepository
                .findAll(pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TecnicoResponse obtenerPorId(UUID id) {

        Tecnico tecnico = buscarTecnico(id);

        return convertirAResponse(tecnico);
    }

    @Override
    @Transactional
    public TecnicoResponse actualizar(
            UUID id,
            TecnicoRequest request
    ) {

        Tecnico tecnico = buscarTecnico(id);

        if (!tecnico
                .getPerfil()
                .getId()
                .equals(request.perfilId())) {

            throw new BusinessRuleException(
                    "No se puede cambiar el perfil asociado al técnico"
            );
        }

        validarCodigoActualizacion(
                tecnico,
                request.codigoTecnico()
        );

        tecnico.setCodigoTecnico(request.codigoTecnico());
        tecnico.setEspecialidad(request.especialidad());

        if (request.activo() != null) {
            tecnico.setActivo(request.activo());
        }

        Tecnico tecnicoActualizado =
                tecnicoRepository.save(tecnico);

        return convertirAResponse(tecnicoActualizado);
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

    private Tecnico buscarTecnico(UUID id) {

        return tecnicoRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "No existe un técnico con el id: "
                                        + id
                        )
                );
    }

    private void validarCodigoActualizacion(
            Tecnico tecnico,
            String nuevoCodigo
    ) {

        tecnicoRepository
                .findAll()
                .stream()
                .filter(encontrado ->
                        nuevoCodigo.equals(
                                encontrado.getCodigoTecnico()
                        )
                )
                .filter(encontrado ->
                        !encontrado
                                .getId()
                                .equals(tecnico.getId())
                )
                .findFirst()
                .ifPresent(encontrado -> {
                    throw new ConflictException(
                            "Ya existe otro técnico con el código: "
                                    + nuevoCodigo
                    );
                });
    }

    private TecnicoResponse convertirAResponse(Tecnico tecnico) {

        return new TecnicoResponse(
                tecnico.getId(),
                tecnico.getPerfil().getId(),
                tecnico.getCodigoTecnico(),
                tecnico.getEspecialidad(),
                tecnico.getActivo(),
                tecnico.getCreadoEn(),
                tecnico.getActualizadoEn()
        );
    }
}