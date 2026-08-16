package ec.edu.scli.usuarios.service.impl;

import ec.edu.scli.usuarios.dto.docente.DocenteRequest;
import ec.edu.scli.usuarios.dto.docente.DocenteResponse;
import ec.edu.scli.usuarios.entity.Docente;
import ec.edu.scli.usuarios.entity.Perfil;
import ec.edu.scli.usuarios.exception.BusinessRuleException;
import ec.edu.scli.usuarios.exception.ConflictException;
import ec.edu.scli.usuarios.exception.ResourceNotFoundException;
import ec.edu.scli.usuarios.repository.DocenteRepository;
import ec.edu.scli.usuarios.repository.PerfilRepository;
import ec.edu.scli.usuarios.service.DocenteService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DocenteServiceImpl implements DocenteService {

    private final DocenteRepository docenteRepository;
    private final PerfilRepository perfilRepository;

    public DocenteServiceImpl(
            DocenteRepository docenteRepository,
            PerfilRepository perfilRepository
    ) {
        this.docenteRepository = docenteRepository;
        this.perfilRepository = perfilRepository;
    }

    @Override
    @Transactional
    public DocenteResponse crear(DocenteRequest request) {

        Perfil perfil = buscarPerfil(request.perfilId());

        if (!perfil.getActivo()) {
            throw new BusinessRuleException(
                    "No se puede crear un docente con un perfil inactivo"
            );
        }

        if (docenteRepository.existsByPerfilId(request.perfilId())) {
            throw new ConflictException(
                    "El perfil ya está registrado como docente"
            );
        }

        validarCodigoDuplicado(request.codigoDocente());

        Docente docente = new Docente();

        docente.setPerfil(perfil);
        docente.setCodigoDocente(request.codigoDocente());
        docente.setTituloAcademico(request.tituloAcademico());
        docente.setDepartamento(request.departamento());
        docente.setTipoContrato(request.tipoContrato());
        docente.setDedicacion(request.dedicacion());

        if (request.activo() == null) {
            docente.setActivo(true);
        } else {
            docente.setActivo(request.activo());
        }

        Docente docenteGuardado =
                docenteRepository.save(docente);

        return convertirAResponse(docenteGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocenteResponse> listar(Pageable pageable) {

        return docenteRepository
                .findAll(pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DocenteResponse obtenerPorId(UUID id) {

        Docente docente = buscarDocente(id);

        return convertirAResponse(docente);
    }

    @Override
    @Transactional
    public DocenteResponse actualizar(
            UUID id,
            DocenteRequest request
    ) {

        Docente docente = buscarDocente(id);

        /*
         * No permitimos cambiar el perfil asociado al docente
         * mediante una actualización.
         */
        if (!docente.getPerfil().getId().equals(request.perfilId())) {
            throw new BusinessRuleException(
                    "No se puede cambiar el perfil asociado al docente"
            );
        }

        validarCodigoActualizacion(
                docente,
                request.codigoDocente()
        );

        docente.setCodigoDocente(request.codigoDocente());
        docente.setTituloAcademico(request.tituloAcademico());
        docente.setDepartamento(request.departamento());
        docente.setTipoContrato(request.tipoContrato());
        docente.setDedicacion(request.dedicacion());

        if (request.activo() != null) {
            docente.setActivo(request.activo());
        }

        Docente docenteActualizado =
                docenteRepository.save(docente);

        return convertirAResponse(docenteActualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public DocenteResponse obtenerPorPerfilId(UUID perfilId) {

        Docente docente = docenteRepository
                .findByPerfilId(perfilId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "No existe un docente asociado al perfil: "
                                        + perfilId
                        )
                );

        return convertirAResponse(docente);
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

    private Docente buscarDocente(UUID id) {

        return docenteRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "No existe un docente con el id: "
                                        + id
                        )
                );
    }

    private void validarCodigoDuplicado(String codigoDocente) {

        if (codigoDocente != null
                && docenteRepository
                .existsByCodigoDocente(codigoDocente)) {

            throw new ConflictException(
                    "Ya existe un docente con el código: "
                            + codigoDocente
            );
        }
    }

    private void validarCodigoActualizacion(
            Docente docente,
            String nuevoCodigo
    ) {

        if (nuevoCodigo == null) {
            return;
        }

        docenteRepository
                .findAll()
                .stream()
                .filter(encontrado ->
                        nuevoCodigo.equals(
                                encontrado.getCodigoDocente()
                        )
                )
                .filter(encontrado ->
                        !encontrado.getId().equals(docente.getId())
                )
                .findFirst()
                .ifPresent(encontrado -> {
                    throw new ConflictException(
                            "Ya existe otro docente con el código: "
                                    + nuevoCodigo
                    );
                });
    }

    private DocenteResponse convertirAResponse(Docente docente) {

        return new DocenteResponse(
                docente.getId(),
                docente.getPerfil().getId(),
                docente.getCodigoDocente(),
                docente.getTituloAcademico(),
                docente.getDepartamento(),
                docente.getTipoContrato(),
                docente.getDedicacion(),
                docente.getActivo(),
                docente.getCreadoEn(),
                docente.getActualizadoEn()
        );
    }
}