package ec.edu.scli.academico.service.impl;

import ec.edu.scli.academico.dto.carrera.CarreraRequest;
import ec.edu.scli.academico.dto.carrera.CarreraResponse;
import ec.edu.scli.academico.entity.Carrera;
import ec.edu.scli.academico.exception.BusinessRuleException;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.repository.CarreraRepository;
import ec.edu.scli.academico.repository.FacultadRepository;
import ec.edu.scli.academico.service.CarreraService;
import ec.edu.scli.academico.specification.CarreraSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CarreraServiceImpl implements CarreraService {

    private final CarreraRepository carreraRepository;
    private final FacultadRepository facultadRepository;

    public CarreraServiceImpl(CarreraRepository carreraRepository, FacultadRepository facultadRepository) {
        this.carreraRepository = carreraRepository;
        this.facultadRepository = facultadRepository;
    }

    @Override
    @Transactional
    public CarreraResponse crear(CarreraRequest request) {

        validarFacultadExiste(request.facultadId());
        validarCodigoDuplicado(request.codigo(), null);

        Carrera carrera = new Carrera();
        carrera.setFacultadId(request.facultadId());
        carrera.setCodigo(request.codigo());
        carrera.setNombre(request.nombre());
        carrera.setDescripcion(request.descripcion());
        carrera.setActivo(true);

        Carrera guardada = carreraRepository.save(carrera);

        return convertirAResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarreraResponse> listar(UUID facultadId, String codigo, String nombre, Boolean activo, Pageable pageable) {

        Specification<Carrera> specification =
                CarreraSpecification.tieneFacultad(facultadId)
                        .and(CarreraSpecification.codigoContiene(codigo))
                        .and(CarreraSpecification.nombreContiene(nombre))
                        .and(CarreraSpecification.tieneEstado(activo));

        return carreraRepository.findAll(specification, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarreraResponse> listarPorFacultad(UUID facultadId) {

        validarFacultadExiste(facultadId);

        return carreraRepository.findByFacultadId(facultadId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CarreraResponse obtenerPorId(UUID id) {
        return convertirAResponse(buscarCarrera(id));
    }

    @Override
    @Transactional
    public CarreraResponse actualizar(UUID id, CarreraRequest request) {

        Carrera carrera = buscarCarrera(id);

        validarFacultadExiste(request.facultadId());
        validarCodigoDuplicado(request.codigo(), id);

        carrera.setFacultadId(request.facultadId());
        carrera.setCodigo(request.codigo());
        carrera.setNombre(request.nombre());
        carrera.setDescripcion(request.descripcion());

        Carrera actualizada = carreraRepository.save(carrera);

        return convertirAResponse(actualizada);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {

        Carrera carrera = buscarCarrera(id);
        carrera.setActivo(false);

        carreraRepository.save(carrera);
    }

    private Carrera buscarCarrera(UUID id) {
        return carreraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una carrera con el id: " + id));
    }

    private void validarFacultadExiste(UUID facultadId) {
        if (!facultadRepository.existsById(facultadId)) {
            throw new BusinessRuleException(
                    "No existe una facultad con el id: " + facultadId);
        }
    }

    private void validarCodigoDuplicado(String codigo, UUID idActual) {

        boolean existe = (idActual == null)
                ? carreraRepository.existsByCodigo(codigo)
                : carreraRepository.existsByCodigoAndIdNot(codigo, idActual);

        if (existe) {
            throw new ConflictException("Ya existe una carrera con el código: " + codigo);
        }
    }

    private CarreraResponse convertirAResponse(Carrera carrera) {
        return new CarreraResponse(
                carrera.getId(),
                carrera.getFacultadId(),
                carrera.getCodigo(),
                carrera.getNombre(),
                carrera.getDescripcion(),
                carrera.isActivo(),
                carrera.getCreadoEn(),
                carrera.getActualizadoEn()
        );
    }
}
