package ec.edu.scli.academico.service.impl;

import ec.edu.scli.academico.dto.piso.PisoRequest;
import ec.edu.scli.academico.dto.piso.PisoResponse;
import ec.edu.scli.academico.entity.Piso;
import ec.edu.scli.academico.exception.BusinessRuleException;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.repository.BloqueRepository;
import ec.edu.scli.academico.repository.PisoRepository;
import ec.edu.scli.academico.service.PisoService;
import ec.edu.scli.academico.specification.PisoSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PisoServiceImpl implements PisoService {

    private final PisoRepository pisoRepository;
    private final BloqueRepository bloqueRepository;

    public PisoServiceImpl(PisoRepository pisoRepository, BloqueRepository bloqueRepository) {
        this.pisoRepository = pisoRepository;
        this.bloqueRepository = bloqueRepository;
    }

    @Override
    @Transactional
    public PisoResponse crear(PisoRequest request) {

        validarBloqueExiste(request.bloqueId());
        validarNumeroDuplicado(request.bloqueId(), request.numero(), null);

        Piso piso = new Piso();
        piso.setBloqueId(request.bloqueId());
        piso.setNumero(request.numero());
        piso.setDescripcion(request.descripcion());
        piso.setActivo(true);

        Piso guardado = pisoRepository.save(piso);

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PisoResponse> listar(UUID bloqueId, Boolean activo, Pageable pageable) {

        Specification<Piso> specification =
                PisoSpecification.tieneBloque(bloqueId)
                        .and(PisoSpecification.tieneEstado(activo));

        return pisoRepository.findAll(specification, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PisoResponse> listarPorBloque(UUID bloqueId) {

        validarBloqueExiste(bloqueId);

        return pisoRepository.findByBloqueId(bloqueId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PisoResponse obtenerPorId(UUID id) {
        return convertirAResponse(buscarPiso(id));
    }

    @Override
    @Transactional
    public PisoResponse actualizar(UUID id, PisoRequest request) {

        Piso piso = buscarPiso(id);

        validarBloqueExiste(request.bloqueId());
        validarNumeroDuplicado(request.bloqueId(), request.numero(), id);

        piso.setBloqueId(request.bloqueId());
        piso.setNumero(request.numero());
        piso.setDescripcion(request.descripcion());

        Piso actualizado = pisoRepository.save(piso);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {

        Piso piso = buscarPiso(id);
        piso.setActivo(false);

        pisoRepository.save(piso);
    }

    private Piso buscarPiso(UUID id) {
        return pisoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un piso con el id: " + id));
    }

    private void validarBloqueExiste(UUID bloqueId) {
        if (!bloqueRepository.existsById(bloqueId)) {
            throw new BusinessRuleException(
                    "No existe un bloque con el id: " + bloqueId);
        }
    }

    private void validarNumeroDuplicado(UUID bloqueId, Integer numero, UUID idActual) {

        boolean existe = (idActual == null)
                ? pisoRepository.existsByBloqueIdAndNumero(bloqueId, numero)
                : pisoRepository.existsByBloqueIdAndNumeroAndIdNot(bloqueId, numero, idActual);

        if (existe) {
            throw new ConflictException(
                    "Ya existe el piso número " + numero + " en ese bloque");
        }
    }

    private PisoResponse convertirAResponse(Piso piso) {
        return new PisoResponse(
                piso.getId(),
                piso.getBloqueId(),
                piso.getNumero(),
                piso.getDescripcion(),
                piso.isActivo(),
                piso.getCreadoEn(),
                piso.getActualizadoEn()
        );
    }
}
