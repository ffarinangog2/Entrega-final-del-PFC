package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.application.service.TipoEquipoService;
import ec.edu.scli.academico.domain.model.TipoEquipo;
import ec.edu.scli.academico.domain.port.TipoEquipoRepositoryPort;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.tipoequipo.TipoEquipoRequest;
import ec.edu.scli.academico.presentation.dto.tipoequipo.TipoEquipoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TipoEquipoServiceImpl implements TipoEquipoService {

    private final TipoEquipoRepositoryPort tipoEquipoRepositoryPort;

    public TipoEquipoServiceImpl(TipoEquipoRepositoryPort tipoEquipoRepositoryPort) {
        this.tipoEquipoRepositoryPort = tipoEquipoRepositoryPort;
    }

    @Override
    @Transactional
    public TipoEquipoResponse crear(TipoEquipoRequest request) {

        validarCodigoDuplicado(request.codigo(), null);

        TipoEquipo tipoEquipo = TipoEquipo.nuevo(request.codigo(), request.nombre(), request.descripcion());

        TipoEquipo guardado = tipoEquipoRepositoryPort.guardar(tipoEquipo);

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TipoEquipoResponse> listar(String codigo, String nombre, Boolean activo, Pageable pageable) {
        return tipoEquipoRepositoryPort.buscar(codigo, nombre, activo, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TipoEquipoResponse obtenerPorId(UUID id) {
        return convertirAResponse(buscarTipoEquipo(id));
    }

    @Override
    @Transactional
    public TipoEquipoResponse actualizar(UUID id, TipoEquipoRequest request) {

        TipoEquipo tipoEquipo = buscarTipoEquipo(id);

        validarCodigoDuplicado(request.codigo(), id);

        tipoEquipo.actualizarDatos(request.codigo(), request.nombre(), request.descripcion());

        TipoEquipo actualizado = tipoEquipoRepositoryPort.guardar(tipoEquipo);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {

        TipoEquipo tipoEquipo = buscarTipoEquipo(id);
        tipoEquipo.desactivar();

        tipoEquipoRepositoryPort.guardar(tipoEquipo);
    }

    private TipoEquipo buscarTipoEquipo(UUID id) {
        return tipoEquipoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un tipo de equipo con el id: " + id));
    }

    private void validarCodigoDuplicado(String codigo, UUID idActual) {

        boolean existe = (idActual == null)
                ? tipoEquipoRepositoryPort.existeCodigo(codigo)
                : tipoEquipoRepositoryPort.existeCodigoParaOtroId(codigo, idActual);

        if (existe) {
            throw new ConflictException("Ya existe un tipo de equipo con el código: " + codigo);
        }
    }

    private TipoEquipoResponse convertirAResponse(TipoEquipo tipoEquipo) {
        return new TipoEquipoResponse(
                tipoEquipo.getId(),
                tipoEquipo.getCodigo(),
                tipoEquipo.getNombre(),
                tipoEquipo.getDescripcion(),
                tipoEquipo.isActivo(),
                tipoEquipo.getCreadoEn(),
                tipoEquipo.getActualizadoEn()
        );
    }
}
