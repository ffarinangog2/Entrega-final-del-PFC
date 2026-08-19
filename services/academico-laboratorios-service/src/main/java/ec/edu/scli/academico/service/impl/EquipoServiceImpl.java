package ec.edu.scli.academico.service.impl;

import ec.edu.scli.academico.dto.equipo.EquipoRequest;
import ec.edu.scli.academico.dto.equipo.EquipoResponse;
import ec.edu.scli.academico.entity.Equipo;
import ec.edu.scli.academico.enums.EstadoEquipo;
import ec.edu.scli.academico.exception.BusinessRuleException;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.repository.EquipoRepository;
import ec.edu.scli.academico.repository.LaboratorioRepository;
import ec.edu.scli.academico.repository.TipoEquipoRepository;
import ec.edu.scli.academico.service.EquipoService;
import ec.edu.scli.academico.specification.EquipoSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EquipoServiceImpl implements EquipoService {

    private final EquipoRepository equipoRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final TipoEquipoRepository tipoEquipoRepository;

    public EquipoServiceImpl(
            EquipoRepository equipoRepository,
            LaboratorioRepository laboratorioRepository,
            TipoEquipoRepository tipoEquipoRepository
    ) {
        this.equipoRepository = equipoRepository;
        this.laboratorioRepository = laboratorioRepository;
        this.tipoEquipoRepository = tipoEquipoRepository;
    }

    @Override
    @Transactional
    public EquipoResponse crear(EquipoRequest request) {

        validarLaboratorioExiste(request.laboratorioId());
        validarTipoEquipoExiste(request.tipoEquipoId());
        validarCodigoInventarioDuplicado(request.codigoInventario(), null);
        validarNumeroSerieDuplicado(request.numeroSerie(), null);

        Equipo equipo = new Equipo();
        aplicarDatos(equipo, request);
        equipo.setEstado(EstadoEquipo.OPERATIVO);
        equipo.setActivo(true);

        Equipo guardado = equipoRepository.save(equipo);

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EquipoResponse> listar(UUID laboratorioId, EstadoEquipo estado, Boolean activo, Pageable pageable) {

        Specification<Equipo> specification =
                EquipoSpecification.tieneLaboratorio(laboratorioId)
                        .and(EquipoSpecification.tieneEstadoEquipo(estado))
                        .and(EquipoSpecification.tieneEstado(activo));

        return equipoRepository.findAll(specification, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoResponse> listarPorLaboratorio(UUID laboratorioId) {

        validarLaboratorioExiste(laboratorioId);

        return equipoRepository.findByLaboratorioId(laboratorioId)
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

        aplicarDatos(equipo, request);

        Equipo actualizado = equipoRepository.save(equipo);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional
    public EquipoResponse cambiarEstado(UUID id, EstadoEquipo estado) {

        Equipo equipo = buscarEquipo(id);
        equipo.setEstado(estado);

        if (estado == EstadoEquipo.FUERA_DE_SERVICIO) {
            equipo.setActivo(false);
        } else if (!equipo.isActivo()) {
            equipo.setActivo(true);
        }

        Equipo actualizado = equipoRepository.save(equipo);

        return convertirAResponse(actualizado);
    }

    private void aplicarDatos(Equipo equipo, EquipoRequest request) {
        equipo.setLaboratorioId(request.laboratorioId());
        equipo.setTipoEquipoId(request.tipoEquipoId());
        equipo.setCodigoInventario(request.codigoInventario());
        equipo.setNumeroSerie(request.numeroSerie());
        equipo.setMarca(request.marca());
        equipo.setModelo(request.modelo());
        equipo.setProcesador(request.procesador());
        equipo.setMemoriaRam(request.memoriaRam());
        equipo.setAlmacenamiento(request.almacenamiento());
        equipo.setDireccionIp(request.direccionIp());
        equipo.setDireccionMac(request.direccionMac());
        equipo.setObservacion(request.observacion());
    }

    private Equipo buscarEquipo(UUID id) {
        return equipoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un equipo con el id: " + id));
    }

    private void validarLaboratorioExiste(UUID laboratorioId) {
        if (!laboratorioRepository.existsById(laboratorioId)) {
            throw new BusinessRuleException(
                    "No existe un laboratorio con el id: " + laboratorioId);
        }
    }

    private void validarTipoEquipoExiste(UUID tipoEquipoId) {
        if (!tipoEquipoRepository.existsById(tipoEquipoId)) {
            throw new BusinessRuleException(
                    "No existe un tipo de equipo con el id: " + tipoEquipoId);
        }
    }

    private void validarCodigoInventarioDuplicado(String codigoInventario, UUID idActual) {

        boolean existe = (idActual == null)
                ? equipoRepository.existsByCodigoInventario(codigoInventario)
                : equipoRepository.existsByCodigoInventarioAndIdNot(codigoInventario, idActual);

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
                ? equipoRepository.existsByNumeroSerie(numeroSerie)
                : equipoRepository.existsByNumeroSerieAndIdNot(numeroSerie, idActual);

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
