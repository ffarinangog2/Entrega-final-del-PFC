package ec.edu.scli.academico.service.impl;

import ec.edu.scli.academico.dto.campus.CampusRequest;
import ec.edu.scli.academico.dto.campus.CampusResponse;
import ec.edu.scli.academico.entity.Campus;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.repository.CampusRepository;
import ec.edu.scli.academico.service.CampusService;
import ec.edu.scli.academico.specification.CampusSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CampusServiceImpl implements CampusService {

    private final CampusRepository campusRepository;

    public CampusServiceImpl(CampusRepository campusRepository) {
        this.campusRepository = campusRepository;
    }

    @Override
    @Transactional
    public CampusResponse crear(CampusRequest request) {

        validarCodigoDuplicado(request.codigo(), null);

        Campus campus = new Campus();
        campus.setCodigo(request.codigo());
        campus.setNombre(request.nombre());
        campus.setDireccion(request.direccion());
        campus.setActivo(true);

        Campus guardado = campusRepository.save(campus);

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CampusResponse> listar(String codigo, String nombre, Boolean activo, Pageable pageable) {

        Specification<Campus> specification =
                CampusSpecification.codigoContiene(codigo)
                        .and(CampusSpecification.nombreContiene(nombre))
                        .and(CampusSpecification.tieneEstado(activo));

        return campusRepository.findAll(specification, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CampusResponse obtenerPorId(UUID id) {
        return convertirAResponse(buscarCampus(id));
    }

    @Override
    @Transactional
    public CampusResponse actualizar(UUID id, CampusRequest request) {

        Campus campus = buscarCampus(id);

        validarCodigoDuplicado(request.codigo(), id);

        campus.setCodigo(request.codigo());
        campus.setNombre(request.nombre());
        campus.setDireccion(request.direccion());

        Campus actualizado = campusRepository.save(campus);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {

        Campus campus = buscarCampus(id);
        campus.setActivo(false);

        campusRepository.save(campus);
    }

    private Campus buscarCampus(UUID id) {
        return campusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un campus con el id: " + id));
    }

    private void validarCodigoDuplicado(String codigo, UUID idActual) {

        boolean existe = (idActual == null)
                ? campusRepository.existsByCodigo(codigo)
                : campusRepository.existsByCodigoAndIdNot(codigo, idActual);

        if (existe) {
            throw new ConflictException("Ya existe un campus con el código: " + codigo);
        }
    }

    private CampusResponse convertirAResponse(Campus campus) {
        return new CampusResponse(
                campus.getId(),
                campus.getCodigo(),
                campus.getNombre(),
                campus.getDireccion(),
                campus.isActivo(),
                campus.getCreadoEn(),
                campus.getActualizadoEn()
        );
    }
}
