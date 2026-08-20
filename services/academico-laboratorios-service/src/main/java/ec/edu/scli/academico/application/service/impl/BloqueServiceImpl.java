package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.application.service.BloqueService;
import ec.edu.scli.academico.domain.model.Bloque;
import ec.edu.scli.academico.domain.port.BloqueRepositoryPort;
import ec.edu.scli.academico.domain.port.CampusRepositoryPort;
import ec.edu.scli.academico.domain.exception.BusinessRuleException;
import ec.edu.scli.academico.domain.exception.ConflictException;
import ec.edu.scli.academico.domain.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.bloque.BloqueRequest;
import ec.edu.scli.academico.presentation.dto.bloque.BloqueResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BloqueServiceImpl implements BloqueService {

    private final BloqueRepositoryPort bloqueRepositoryPort;
    private final CampusRepositoryPort campusRepositoryPort;

    public BloqueServiceImpl(
            BloqueRepositoryPort bloqueRepositoryPort,
            CampusRepositoryPort campusRepositoryPort
    ) {
        this.bloqueRepositoryPort = bloqueRepositoryPort;
        this.campusRepositoryPort = campusRepositoryPort;
    }

    @Override
    @Transactional
    public BloqueResponse crear(BloqueRequest request) {

        validarCampusExiste(request.campusId());
        validarCodigoDuplicado(request.campusId(), request.codigo(), null);

        Bloque bloque = Bloque.nuevo(request.campusId(), request.codigo(), request.nombre());

        Bloque guardado = bloqueRepositoryPort.guardar(bloque);

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BloqueResponse> listar(UUID campusId, String nombre, Boolean activo, Pageable pageable) {
        return bloqueRepositoryPort.buscar(campusId, nombre, activo, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BloqueResponse> listarPorCampus(UUID campusId) {

        validarCampusExiste(campusId);

        return bloqueRepositoryPort.buscarPorCampus(campusId)
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
        validarCodigoDuplicado(request.campusId(), request.codigo(), id);

        bloque.actualizarDatos(request.campusId(), request.codigo(), request.nombre());

        Bloque actualizado = bloqueRepositoryPort.guardar(bloque);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {

        Bloque bloque = buscarBloque(id);
        bloque.desactivar();

        bloqueRepositoryPort.guardar(bloque);
    }

    private Bloque buscarBloque(UUID id) {
        return bloqueRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un bloque con el id: " + id));
    }

    private void validarCampusExiste(UUID campusId) {
        if (campusRepositoryPort.buscarPorId(campusId).isEmpty()) {
            throw new BusinessRuleException(
                    "No existe un campus con el id: " + campusId);
        }
    }

    private void validarCodigoDuplicado(UUID campusId, String codigo, UUID idActual) {

        boolean existe = (idActual == null)
                ? bloqueRepositoryPort.existeCodigoEnCampus(campusId, codigo)
                : bloqueRepositoryPort.existeCodigoEnCampusParaOtroId(campusId, codigo, idActual);

        if (existe) {
            throw new ConflictException(
                    "Ya existe un bloque con el código '" + codigo + "' en ese campus");
        }
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
