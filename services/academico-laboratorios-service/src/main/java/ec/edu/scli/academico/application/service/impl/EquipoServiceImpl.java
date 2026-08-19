package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.application.service.EquipoService;
import ec.edu.scli.academico.domain.model.Equipo;
import ec.edu.scli.academico.domain.port.EquipoRepositoryPort;
import ec.edu.scli.academico.domain.port.LaboratorioRepositoryPort;
import ec.edu.scli.academico.domain.port.TipoEquipoRepositoryPort;
import ec.edu.scli.academico.enums.EstadoEquipo;
import ec.edu.scli.academico.exception.BusinessRuleException;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.equipo.EquipoRequest;
import ec.edu.scli.academico.presentation.dto.equipo.EquipoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EquipoServiceImpl implements EquipoService {

    private final EquipoRepositoryPort equipoRepositoryPort;
    private final LaboratorioRepositoryPort laboratorioRepositoryPort;
    private final TipoEquipoRepositoryPort tipoEquipoRepositoryPort;

    public EquipoServiceImpl(
            EquipoRepositoryPort equipoRepositoryPort,
            LaboratorioRepositoryPort laboratorioRepositoryPort,
            TipoEquipoRepositoryPort tipoEquipoRepositoryPort
    ) {
        this.equipoRepositoryPort = equipoRepositoryPort;
        this.laboratorioRepositoryPort = laboratorioRepositoryPort;
        this.tipoEquipoRepositoryPort = tipoEquipoRepositoryPort;
    }

    @Override
    @Transactional
    public EquipoResponse crear(EquipoRequest request) {

        validarLaboratorioExiste(request.laboratorioId());
        validarTipoEquipoExiste(request.tipoEquipoId());
        validarCodigoInventarioDuplicado(request.codigoInventario(), null);
        validarNumeroSerieDuplicado(request.numeroSerie(), null);

        Equipo equipo = Equipo.nuevo(
                request.laboratorioId(),
                request.tipoEquipoId(),
                request.codigoInventario(),
                request.numeroSerie(),
                request.marca(),
                request.modelo(),
                request.procesador(),
                request.memoriaRam(),
                request.almacenamiento(),
                request.direccionIp(),
                request.direccionMac(),
                request.observacion()
        );

        Equipo guardado = equipoRepositoryPort.guardar(equipo);

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EquipoResponse> listar(UUID laboratorioId, EstadoEquipo estado, Boolean activo, Pageable pageable) {
        return equipoRepositoryPort.buscar(laboratorioId, estado, activo, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoResponse> listarPorLaboratorio(UUID laboratorioId) {

        validarLaboratorioExiste(laboratorioId);

        return equipoRepositoryPort.buscarPorLaboratorio(laboratorioId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EquipoResponse obtenerPorId(UUID id) {
        return convertirAResponse(buscarEquipo(id));
    }

    @Override
    @Transactional
    public EquipoResponse actualizar(UUID id, EquipoRequest request) {

        Equipo equipo = buscarEquipo(id);

        validarLaboratorioExiste(request.laboratorioId());
        validarTipoEquipoExiste(request.tipoEquipoId());
        validarCodigoInventarioDuplicado(request.codigoInventario(), id);
        validarNumeroSerieDuplicado(request.numeroSerie(), id);

        equipo.aplicarDatos(
                request.laboratorioId(),
                request.tipoEquipoId(),
                request.codigoInventario(),
                request.numeroSerie(),
                request.marca(),
                request.modelo(),
                request.procesador(),
                request.memoriaRam(),
                request.almacenamiento(),
                request.direccionIp(),
                request.direccionMac(),
                request.observacion()
        );

        Equipo actualizado = equipoRepositoryPort.guardar(equipo);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional
    public EquipoResponse cambiarEstado(UUID id, EstadoEquipo estado) {

        Equipo equipo = buscarEquipo(id);
        equipo.cambiarEstado(estado);

        Equipo actualizado = equipoRepositoryPort.guardar(equipo);

        return convertirAResponse(actualizado);
    }

    private Equipo buscarEquipo(UUID id) {
        return equipoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un equipo con el id: " + id));
    }

    private void validarLaboratorioExiste(UUID laboratorioId) {
        if (laboratorioRepositoryPort.buscarPorId(laboratorioId).isEmpty()) {
            throw new BusinessRuleException(
                    "No existe un laboratorio con el id: " + laboratorioId);
        }
    }

    private void validarTipoEquipoExiste(UUID tipoEquipoId) {
        if (!tipoEquipoRepositoryPort.existePorId(tipoEquipoId)) {
            throw new BusinessRuleException(
                    "No existe un tipo de equipo con el id: " + tipoEquipoId);
        }
    }

    private void validarCodigoInventarioDuplicado(String codigoInventario, UUID idActual) {

        boolean existe = (idActual == null)
                ? equipoRepositoryPort.existeCodigoInventario(codigoInventario)
                : equipoRepositoryPort.existeCodigoInventarioParaOtroId(codigoInventario, idActual);

        if (existe) {
            throw new ConflictException(
                    "Ya existe un equipo con el código de inventario: " + codigoInventario);
        }
    }

    private void validarNumeroSerieDuplicado(String numeroSerie, UUID idActual) {

        if (numeroSerie == null || numeroSerie.isBlank()) {
            return;
        }

        boolean existe = (idActual == null)
                ? equipoRepositoryPort.existeNumeroSerie(numeroSerie)
                : equipoRepositoryPort.existeNumeroSerieParaOtroId(numeroSerie, idActual);

        if (existe) {
            throw new ConflictException(
                    "Ya existe un equipo con el número de serie: " + numeroSerie);
        }
    }

    private EquipoResponse convertirAResponse(Equipo equipo) {
        return new EquipoResponse(
                equipo.getId(),
                equipo.getLaboratorioId(),
                equipo.getTipoEquipoId(),
                equipo.getCodigoInventario(),
                equipo.getNumeroSerie(),
                equipo.getMarca(),
                equipo.getModelo(),
                equipo.getProcesador(),
                equipo.getMemoriaRam(),
                equipo.getAlmacenamiento(),
                equipo.getDireccionIp(),
                equipo.getDireccionMac(),
                equipo.getEstado(),
                equipo.getObservacion(),
                equipo.isActivo(),
                equipo.getCreadoEn(),
                equipo.getActualizadoEn()
        );
    }
}
