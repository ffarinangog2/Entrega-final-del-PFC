package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.application.service.CampusService;
import ec.edu.scli.academico.domain.model.Campus;
import ec.edu.scli.academico.domain.port.CampusRepositoryPort;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.campus.CampusRequest;
import ec.edu.scli.academico.presentation.dto.campus.CampusResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso de Campus. A diferencia de la versión anterior, esta clase
 * ya no importa nada de JPA/Spring Data más allá de Page/Pageable: toda
 * la persistencia pasa por CampusRepositoryPort, cumpliendo con la regla
 * de dependencia hacia adentro (application -> domain, nunca al revés).
 */
@Service
public class CampusServiceImpl implements CampusService {

    private final CampusRepositoryPort campusRepositoryPort;

    public CampusServiceImpl(CampusRepositoryPort campusRepositoryPort) {
        this.campusRepositoryPort = campusRepositoryPort;
    }

    @Override
    @Transactional
    public CampusResponse crear(CampusRequest request) {

        validarCodigoDuplicado(request.codigo(), null);

        Campus campus = Campus.nuevo(request.codigo(), request.nombre(), request.direccion());

        Campus guardado = campusRepositoryPort.guardar(campus);

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CampusResponse> listar(String codigo, String nombre, Boolean activo, Pageable pageable) {
        return campusRepositoryPort.buscar(codigo, nombre, activo, pageable)
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

        campus.actualizarDatos(request.codigo(), request.nombre(), request.direccion());

        Campus actualizado = campusRepositoryPort.guardar(campus);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {

        Campus campus = buscarCampus(id);
        campus.desactivar();

        campusRepositoryPort.guardar(campus);
    }

    private Campus buscarCampus(UUID id) {
        return campusRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un campus con el id: " + id));
    }

    private void validarCodigoDuplicado(String codigo, UUID idActual) {

        boolean existe = (idActual == null)
                ? campusRepositoryPort.existeCodigo(codigo)
                : campusRepositoryPort.existeCodigoParaOtroId(codigo, idActual);

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
