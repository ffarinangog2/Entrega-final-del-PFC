package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.application.service.LaboratorioService;
import ec.edu.scli.academico.domain.model.Laboratorio;
import ec.edu.scli.academico.domain.port.LaboratorioRepositoryPort;
import ec.edu.scli.academico.domain.port.PisoRepositoryPort;
import ec.edu.scli.academico.dto.internal.ExisteResponse;
import ec.edu.scli.academico.dto.internal.LaboratorioDisponibilidadBaseResponse;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import ec.edu.scli.academico.domain.exception.BusinessRuleException;
import ec.edu.scli.academico.domain.exception.ConflictException;
import ec.edu.scli.academico.domain.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioRequest;
import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LaboratorioServiceImpl implements LaboratorioService {

    private final LaboratorioRepositoryPort laboratorioRepositoryPort;
    private final PisoRepositoryPort pisoRepositoryPort;

    public LaboratorioServiceImpl(
            LaboratorioRepositoryPort laboratorioRepositoryPort,
            PisoRepositoryPort pisoRepositoryPort
    ) {
        this.laboratorioRepositoryPort = laboratorioRepositoryPort;
        this.pisoRepositoryPort = pisoRepositoryPort;
    }

    @Override
    @Transactional
    public LaboratorioResponse crear(LaboratorioRequest request) {

        validarPisoExiste(request.pisoId());
        validarCodigoDuplicado(request.codigo(), null);

        Laboratorio laboratorio = Laboratorio.nuevo(
                request.pisoId(),
                request.codigo(),
                request.nombre(),
                request.capacidad(),
                request.descripcion()
        );

        Laboratorio guardado = laboratorioRepositoryPort.guardar(laboratorio);

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LaboratorioResponse> listar(String texto, EstadoLaboratorio estado, Boolean activo, Pageable pageable) {
        return laboratorioRepositoryPort.buscar(texto, estado, activo, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LaboratorioResponse> listarDisponibles() {
        return laboratorioRepositoryPort.buscarDisponibles()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LaboratorioResponse obtenerPorId(UUID id) {
        return convertirAResponse(buscarLaboratorio(id));
    }

    @Override
    @Transactional
    public LaboratorioResponse actualizar(UUID id, LaboratorioRequest request) {

        Laboratorio laboratorio = buscarLaboratorio(id);

        validarPisoExiste(request.pisoId());
        validarCodigoDuplicado(request.codigo(), id);

        laboratorio.actualizarDatos(
                request.pisoId(),
                request.codigo(),
                request.nombre(),
                request.capacidad(),
                request.descripcion()
        );

        Laboratorio actualizado = laboratorioRepositoryPort.guardar(laboratorio);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional
    public LaboratorioResponse cambiarEstado(UUID id, EstadoLaboratorio estado) {

        Laboratorio laboratorio = buscarLaboratorio(id);
        laboratorio.cambiarEstado(estado);

        Laboratorio actualizado = laboratorioRepositoryPort.guardar(laboratorio);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public LaboratorioDisponibilidadBaseResponse obtenerDisponibilidadBase(UUID id) {

        return laboratorioRepositoryPort.buscarPorId(id)
                .map(laboratorio -> new LaboratorioDisponibilidadBaseResponse(
                        laboratorio.getId(),
                        true,
                        laboratorio.isActivo(),
                        laboratorio.getEstado(),
                        laboratorio.getCapacidad()
                ))
                .orElseGet(() -> new LaboratorioDisponibilidadBaseResponse(
                        id, false, false, null, null
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public ExisteResponse verificarExistencia(UUID id) {
        return new ExisteResponse(id, laboratorioRepositoryPort.existePorId(id));
    }

    private Laboratorio buscarLaboratorio(UUID id) {
        return laboratorioRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un laboratorio con el id: " + id));
    }

    private void validarPisoExiste(UUID pisoId) {
        if (pisoRepositoryPort.buscarPorId(pisoId).isEmpty()) {
            throw new BusinessRuleException(
                    "No existe un piso con el id: " + pisoId);
        }
    }

    private void validarCodigoDuplicado(String codigo, UUID idActual) {

        boolean existe = (idActual == null)
                ? laboratorioRepositoryPort.existeCodigo(codigo)
                : laboratorioRepositoryPort.existeCodigoParaOtroId(codigo, idActual);

        if (existe) {
            throw new ConflictException("Ya existe un laboratorio con el código: " + codigo);
        }
    }

    private LaboratorioResponse convertirAResponse(Laboratorio laboratorio) {
        return new LaboratorioResponse(
                laboratorio.getId(),
                laboratorio.getPisoId(),
                laboratorio.getCodigo(),
                laboratorio.getNombre(),
                laboratorio.getCapacidad(),
                laboratorio.getDescripcion(),
                laboratorio.getEstado(),
                laboratorio.isActivo(),
                laboratorio.getCreadoEn(),
                laboratorio.getActualizadoEn()
        );
    }
}
