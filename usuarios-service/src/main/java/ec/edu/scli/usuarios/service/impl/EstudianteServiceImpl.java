package ec.edu.scli.usuarios.service.impl;

import ec.edu.scli.usuarios.dto.estudiante.EstudianteRequest;
import ec.edu.scli.usuarios.dto.estudiante.EstudianteResponse;
import ec.edu.scli.usuarios.entity.Estudiante;
import ec.edu.scli.usuarios.entity.Perfil;
import ec.edu.scli.usuarios.exception.BusinessRuleException;
import ec.edu.scli.usuarios.exception.ConflictException;
import ec.edu.scli.usuarios.exception.ResourceNotFoundException;
import ec.edu.scli.usuarios.repository.EstudianteRepository;
import ec.edu.scli.usuarios.repository.PerfilRepository;
import ec.edu.scli.usuarios.service.EstudianteService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EstudianteServiceImpl implements EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final PerfilRepository perfilRepository;

    public EstudianteServiceImpl(
            EstudianteRepository estudianteRepository,
            PerfilRepository perfilRepository
    ) {
        this.estudianteRepository = estudianteRepository;
        this.perfilRepository = perfilRepository;
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

        return convertirAResponse(estudianteGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EstudianteResponse> listar(Pageable pageable) {

        return estudianteRepository
                .findAll(pageable)
                .map(this::convertirAResponse);
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

        if (request.activo() != null) {
            estudiante.setActivo(request.activo());
        }

        Estudiante estudianteActualizado =
                estudianteRepository.save(estudiante);

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
}