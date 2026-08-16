package ec.edu.scli.academico.service.impl;

import ec.edu.scli.academico.dto.bloque.BloqueRequest;
import ec.edu.scli.academico.dto.bloque.BloqueResponse;
import ec.edu.scli.academico.entity.Bloque;
import ec.edu.scli.academico.exception.BusinessRuleException;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.repository.BloqueRepository;
import ec.edu.scli.academico.repository.CampusRepository;
import ec.edu.scli.academico.service.BloqueService;
import ec.edu.scli.academico.specification.BloqueSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BloqueServiceImpl implements BloqueService {

    private final BloqueRepository bloqueRepository;
    private final CampusRepository campusRepository;

    public BloqueServiceImpl(BloqueRepository bloqueRepository, CampusRepository campusRepository) {
        this.bloqueRepository = bloqueRepository;
        this.campusRepository = campusRepository;
    }

    @Override
    @Transactional
    public BloqueResponse crear(BloqueRequest request) {

        validarCampusExiste(request.campusId());
        validarCodigoDuplicado(request.campusId(), request.codigo(), null);

        Bloque bloque = new Bloque();
        bloque.setCampusId(request.campusId());
        bloque.setCodigo(request.codigo());
        bloque.setNombre(request.nombre());
        bloque.setActivo(true);

        Bloque guardado = bloqueRepository.save(bloque);

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BloqueResponse> listar(UUID campusId, String nombre, Boolean activo, Pageable pageable) {

        Specification<Bloque> specification =
                BloqueSpecification.tieneCampus(campusId)
                        .and(BloqueSpecification.nombreContiene(nombre))
                        .and(BloqueSpecification.tieneEstado(activo));

        return bloqueRepository.findAll(specification, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BloqueResponse> listarPorCampus(UUID campusId) {

        validarCampusExiste(campusId);

        return bloqueRepository.findByCampusId(campusId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BloqueResponse obtenerPorId(UUID id) {
        return convertirAResponse(buscarBloque(id));
    }

    @Override
    @Transactional
    public BloqueResponse actualizar(UUID id, BloqueRequest request) {

        Bloque bloque = buscarBloque(id);

        validarCampusExiste(request.campusId());
        validarCodigoDuplicadoEnActualizacion(bloque, request);

        bloque.setCampusId(request.campusId());
        bloque.setCodigo(request.codigo());
        bloque.setNombre(request.nombre());

        Bloque actualizado = bloqueRepository.save(bloque);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {

        Bloque bloque = buscarBloque(id);
        bloque.setActivo(false);

        bloqueRepository.save(bloque);
    }

    private Bloque buscarBloque(UUID id) {
        return bloqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un bloque con el id: " + id));
    }

    private void validarCampusExiste(UUID campusId) {
        if (!campusRepository.existsById(campusId)) {
            throw new BusinessRuleException(
                    "No existe un campus con el id: " + campusId);
        }
    }

    private void validarCodigoDuplicado(UUID campusId, String codigo, UUID idActual) {

        boolean existe = (idActual == null)
                ? bloqueRepository.existsByCampusIdAndCodigo(campusId, codigo)
                : bloqueRepository.existsByCampusIdAndCodigoAndIdNot(campusId, codigo, idActual);

        if (existe) {
            throw new ConflictException(
                    "Ya existe un bloque con el código '" + codigo + "' en ese campus");
        }
    }

    private void validarCodigoDuplicadoEnActualizacion(Bloque bloque, BloqueRequest request) {
        validarCodigoDuplicado(request.campusId(), request.codigo(), bloque.getId());
    }

    private BloqueResponse convertirAResponse(Bloque bloque) {
        return new BloqueResponse(
                bloque.getId(),
                bloque.getCampusId(),
                bloque.getCodigo(),
                bloque.getNombre(),
                bloque.isActivo(),
                bloque.getCreadoEn(),
                bloque.getActualizadoEn()
        );
    }
}
