package ec.edu.scli.academico.service.impl;

import ec.edu.scli.academico.dto.tipoequipo.TipoEquipoRequest;
import ec.edu.scli.academico.dto.tipoequipo.TipoEquipoResponse;
import ec.edu.scli.academico.entity.TipoEquipo;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.repository.TipoEquipoRepository;
import ec.edu.scli.academico.service.TipoEquipoService;
import ec.edu.scli.academico.specification.TipoEquipoSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TipoEquipoServiceImpl implements TipoEquipoService {

    private final TipoEquipoRepository tipoEquipoRepository;

    public TipoEquipoServiceImpl(TipoEquipoRepository tipoEquipoRepository) {
        this.tipoEquipoRepository = tipoEquipoRepository;
    }

    @Override
    @Transactional
    public TipoEquipoResponse crear(TipoEquipoRequest request) {

        validarCodigoDuplicado(request.codigo(), null);

        TipoEquipo tipoEquipo = new TipoEquipo();
        tipoEquipo.setCodigo(request.codigo());
        tipoEquipo.setNombre(request.nombre());
        tipoEquipo.setDescripcion(request.descripcion());
        tipoEquipo.setActivo(true);

        TipoEquipo guardado = tipoEquipoRepository.save(tipoEquipo);

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TipoEquipoResponse> listar(String codigo, String nombre, Boolean activo, Pageable pageable) {

        Specification<TipoEquipo> specification =
                TipoEquipoSpecification.codigoContiene(codigo)
                        .and(TipoEquipoSpecification.nombreContiene(nombre))
                        .and(TipoEquipoSpecification.tieneEstado(activo));

        return tipoEquipoRepository.findAll(specification, pageable)
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

        tipoEquipo.setCodigo(request.codigo());
        tipoEquipo.setNombre(request.nombre());
        tipoEquipo.setDescripcion(request.descripcion());

        TipoEquipo actualizado = tipoEquipoRepository.save(tipoEquipo);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {

        TipoEquipo tipoEquipo = buscarTipoEquipo(id);
        tipoEquipo.setActivo(false);

        tipoEquipoRepository.save(tipoEquipo);
    }

    private TipoEquipo buscarTipoEquipo(UUID id) {
        return tipoEquipoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un tipo de equipo con el id: " + id));
    }

    private void validarCodigoDuplicado(String codigo, UUID idActual) {

        boolean existe = (idActual == null)
                ? tipoEquipoRepository.existsByCodigo(codigo)
                : tipoEquipoRepository.existsByCodigoAndIdNot(codigo, idActual);

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
