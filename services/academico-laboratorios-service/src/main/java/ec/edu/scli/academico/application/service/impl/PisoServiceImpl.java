package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.application.service.PisoService;
import ec.edu.scli.academico.domain.model.Piso;
import ec.edu.scli.academico.domain.port.PisoRepositoryPort;
import ec.edu.scli.academico.exception.BusinessRuleException;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.piso.PisoRequest;
import ec.edu.scli.academico.presentation.dto.piso.PisoResponse;
import ec.edu.scli.academico.repository.BloqueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PisoServiceImpl implements PisoService {

    private final PisoRepositoryPort pisoRepositoryPort;
    private final BloqueRepository bloqueRepository;

    public PisoServiceImpl(PisoRepositoryPort pisoRepositoryPort, BloqueRepository bloqueRepository) {
        this.pisoRepositoryPort = pisoRepositoryPort;
        this.bloqueRepository = bloqueRepository;
    }

    @Override
    @Transactional
    public PisoResponse crear(PisoRequest request) {

        validarBloqueExiste(request.bloqueId());
        validarNumeroDuplicado(request.bloqueId(), request.numero(), null);

        Piso piso = Piso.nuevo(request.bloqueId(), request.numero(), request.descripcion());

        Piso guardado = pisoRepositoryPort.guardar(piso);

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PisoResponse> listar(UUID bloqueId, Boolean activo, Pageable pageable) {
        return pisoRepositoryPort.buscar(bloqueId, activo, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PisoResponse> listarPorBloque(UUID bloqueId) {

        validarBloqueExiste(bloqueId);

        return pisoRepositoryPort.buscarPorBloque(bloqueId)
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

        piso.actualizarDatos(request.bloqueId(), request.numero(), request.descripcion());

        Piso actualizado = pisoRepositoryPort.guardar(piso);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {

        Piso piso = buscarPiso(id);
        piso.desactivar();

        pisoRepositoryPort.guardar(piso);
    }

    private Piso buscarPiso(UUID id) {
        return pisoRepositoryPort.buscarPorId(id)
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
                ? pisoRepositoryPort.existeNumeroEnBloque(bloqueId, numero)
                : pisoRepositoryPort.existeNumeroEnBloqueParaOtroId(bloqueId, numero, idActual);

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
