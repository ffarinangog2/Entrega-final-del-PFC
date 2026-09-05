package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.application.service.CarreraService;
import ec.edu.scli.academico.domain.model.Carrera;
import ec.edu.scli.academico.domain.port.CarreraRepositoryPort;
import ec.edu.scli.academico.domain.port.FacultadRepositoryPort;
import ec.edu.scli.academico.domain.exception.BusinessRuleException;
import ec.edu.scli.academico.domain.exception.ConflictException;
import ec.edu.scli.academico.domain.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.carrera.CarreraRequest;
import ec.edu.scli.academico.presentation.dto.carrera.CarreraResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * A diferencia de BloqueServiceImpl/MateriaServiceImpl (que todavía no
 * están refactorizados y usan un puente directo a FacultadJpaRepository),
 * esta clase ya depende de FacultadRepositoryPort: como Facultad quedó
 * migrada en el commit anterior, Carrera puede consumirla correctamente
 * a través de su puerto de dominio en vez de la infraestructura.
 */
@Service
public class CarreraServiceImpl implements CarreraService {

    private final CarreraRepositoryPort carreraRepositoryPort;
    private final FacultadRepositoryPort facultadRepositoryPort;

    public CarreraServiceImpl(
            CarreraRepositoryPort carreraRepositoryPort,
            FacultadRepositoryPort facultadRepositoryPort
    ) {
        this.carreraRepositoryPort = carreraRepositoryPort;
        this.facultadRepositoryPort = facultadRepositoryPort;
    }

    @Override
    @Transactional
    public CarreraResponse crear(CarreraRequest request) {

        validarFacultadExiste(request.facultadId());
        validarCodigoDuplicado(request.codigo(), null);

        Carrera carrera = Carrera.nueva(
                request.facultadId(),
                request.codigo(),
                request.nombre(),
                request.descripcion()
        );

        Carrera guardada = carreraRepositoryPort.guardar(carrera);

        return convertirAResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
        public Page<CarreraResponse> listar(
            UUID facultadId, String codigo, String nombre, Boolean activo, Pageable pageable) {
        return carreraRepositoryPort.buscar(facultadId, codigo, nombre, activo, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarreraResponse> listarPorFacultad(UUID facultadId) {

        validarFacultadExiste(facultadId);

        return carreraRepositoryPort.buscarPorFacultad(facultadId)
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

        carrera.actualizarDatos(
                request.facultadId(),
                request.codigo(),
                request.nombre(),
                request.descripcion()
        );

        Carrera actualizada = carreraRepositoryPort.guardar(carrera);

        return convertirAResponse(actualizada);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {

        Carrera carrera = buscarCarrera(id);
        carrera.desactivar();

        carreraRepositoryPort.guardar(carrera);
    }

    private Carrera buscarCarrera(UUID id) {
        return carreraRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una carrera con el id: " + id));
    }

    private void validarFacultadExiste(UUID facultadId) {
        if (!facultadRepositoryPort.existePorId(facultadId)) {
            throw new BusinessRuleException(
                    "No existe una facultad con el id: " + facultadId);
        }
    }

    private void validarCodigoDuplicado(String codigo, UUID idActual) {

        boolean existe = (idActual == null)
                ? carreraRepositoryPort.existeCodigo(codigo)
                : carreraRepositoryPort.existeCodigoParaOtroId(codigo, idActual);

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
