package ec.edu.scli.academico.service.impl;

import ec.edu.scli.academico.dto.facultad.FacultadRequest;
import ec.edu.scli.academico.dto.facultad.FacultadResponse;
import ec.edu.scli.academico.entity.Facultad;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.repository.FacultadRepository;
import ec.edu.scli.academico.service.FacultadService;
import ec.edu.scli.academico.specification.FacultadSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FacultadServiceImpl implements FacultadService {

    private final FacultadRepository facultadRepository;

    public FacultadServiceImpl(FacultadRepository facultadRepository) {
        this.facultadRepository = facultadRepository;
    }

    @Override
    @Transactional
    public FacultadResponse crear(FacultadRequest request) {

        validarCodigoDuplicado(request.codigo(), null);

        Facultad facultad = new Facultad();
        facultad.setCodigo(request.codigo());
        facultad.setNombre(request.nombre());
        facultad.setDescripcion(request.descripcion());
        facultad.setActivo(true);

        Facultad guardada = facultadRepository.save(facultad);

        return convertirAResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FacultadResponse> listar(String codigo, String nombre, Boolean activo, Pageable pageable) {

        Specification<Facultad> specification =
                FacultadSpecification.codigoContiene(codigo)
                        .and(FacultadSpecification.nombreContiene(nombre))
                        .and(FacultadSpecification.tieneEstado(activo));

        return facultadRepository.findAll(specification, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FacultadResponse obtenerPorId(UUID id) {
        return convertirAResponse(buscarFacultad(id));
    }

    @Override
    @Transactional
    public FacultadResponse actualizar(UUID id, FacultadRequest request) {

        Facultad facultad = buscarFacultad(id);

        validarCodigoDuplicado(request.codigo(), id);

        facultad.setCodigo(request.codigo());
        facultad.setNombre(request.nombre());
        facultad.setDescripcion(request.descripcion());

        Facultad actualizada = facultadRepository.save(facultad);

        return convertirAResponse(actualizada);
    }

    @Override
    @Transactional
    public FacultadResponse cambiarEstado(UUID id, Boolean activo) {

        Facultad facultad = buscarFacultad(id);
        facultad.setActivo(activo);

        Facultad actualizada = facultadRepository.save(facultad);

        return convertirAResponse(actualizada);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {

        Facultad facultad = buscarFacultad(id);
        facultad.setActivo(false);

        facultadRepository.save(facultad);
    }

    private Facultad buscarFacultad(UUID id) {
        return facultadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una facultad con el id: " + id));
    }

    private void validarCodigoDuplicado(String codigo, UUID idActual) {

        boolean existe = (idActual == null)
                ? facultadRepository.existsByCodigo(codigo)
                : facultadRepository.existsByCodigoAndIdNot(codigo, idActual);

        if (existe) {
            throw new ConflictException("Ya existe una facultad con el código: " + codigo);
        }
    }

    private FacultadResponse convertirAResponse(Facultad facultad) {
        return new FacultadResponse(
                facultad.getId(),
                facultad.getCodigo(),
                facultad.getNombre(),
                facultad.getDescripcion(),
                facultad.isActivo(),
                facultad.getCreadoEn(),
                facultad.getActualizadoEn()
        );
    }
}
