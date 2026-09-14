package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.application.service.FacultadService;
import ec.edu.scli.academico.domain.model.Facultad;
import ec.edu.scli.academico.domain.port.FacultadRepositoryPort;
import ec.edu.scli.academico.domain.exception.ConflictException;
import ec.edu.scli.academico.domain.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.facultad.FacultadRequest;
import ec.edu.scli.academico.presentation.dto.facultad.FacultadResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FacultadServiceImpl implements FacultadService {

    private final FacultadRepositoryPort facultadRepositoryPort;

    public FacultadServiceImpl(FacultadRepositoryPort facultadRepositoryPort) {
        this.facultadRepositoryPort = facultadRepositoryPort;
    }

    @Override
    @Transactional
    public FacultadResponse crear(FacultadRequest request) {

        validarCodigoDuplicado(request.codigo(), null);

        Facultad facultad = Facultad.nueva(request.codigo(), request.nombre(), request.descripcion());

        Facultad guardada = facultadRepositoryPort.guardar(facultad);

        return convertirAResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FacultadResponse> listar(String codigo, String nombre, Boolean activo, Pageable pageable) {
        return facultadRepositoryPort.buscar(codigo, nombre, activo, pageable)
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

        facultad.actualizarDatos(request.codigo(), request.nombre(), request.descripcion());

        Facultad actualizada = facultadRepositoryPort.guardar(facultad);

        return convertirAResponse(actualizada);
    }

    @Override
    @Transactional
    public FacultadResponse cambiarEstado(UUID id, Boolean activo) {

        Facultad facultad = buscarFacultad(id);
        facultad.cambiarEstado(activo);

        Facultad actualizada = facultadRepositoryPort.guardar(facultad);

        return convertirAResponse(actualizada);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {

        Facultad facultad = buscarFacultad(id);
        facultad.cambiarEstado(false);

        facultadRepositoryPort.guardar(facultad);
    }

    private Facultad buscarFacultad(UUID id) {
        return facultadRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una facultad con el id: " + id));
    }

    private void validarCodigoDuplicado(String codigo, UUID idActual) {

        boolean existe = (idActual == null)
                ? facultadRepositoryPort.existeCodigo(codigo)
                : facultadRepositoryPort.existeCodigoParaOtroId(codigo, idActual);

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
