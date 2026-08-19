package ec.edu.scli.academico.service.impl;

import ec.edu.scli.academico.dto.internal.ExisteResponse;
import ec.edu.scli.academico.dto.internal.LaboratorioDisponibilidadBaseResponse;
import ec.edu.scli.academico.dto.laboratorio.LaboratorioRequest;
import ec.edu.scli.academico.dto.laboratorio.LaboratorioResponse;
import ec.edu.scli.academico.entity.Laboratorio;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import ec.edu.scli.academico.exception.BusinessRuleException;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.repository.LaboratorioRepository;
import ec.edu.scli.academico.repository.PisoRepository;
import ec.edu.scli.academico.service.LaboratorioService;
import ec.edu.scli.academico.specification.LaboratorioSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LaboratorioServiceImpl implements LaboratorioService {

    private final LaboratorioRepository laboratorioRepository;
    private final PisoRepository pisoRepository;

    public LaboratorioServiceImpl(LaboratorioRepository laboratorioRepository, PisoRepository pisoRepository) {
        this.laboratorioRepository = laboratorioRepository;
        this.pisoRepository = pisoRepository;
    }

    @Override
    @Transactional
    public LaboratorioResponse crear(LaboratorioRequest request) {

        validarPisoExiste(request.pisoId());
        validarCodigoDuplicado(request.codigo(), null);

        Laboratorio laboratorio = new Laboratorio();
        laboratorio.setPisoId(request.pisoId());
        laboratorio.setCodigo(request.codigo());
        laboratorio.setNombre(request.nombre());
        laboratorio.setCapacidad(request.capacidad());
        laboratorio.setDescripcion(request.descripcion());
        laboratorio.setEstado(EstadoLaboratorio.DISPONIBLE);
        laboratorio.setActivo(true);

        Laboratorio guardado = laboratorioRepository.save(laboratorio);

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LaboratorioResponse> listar(String texto, EstadoLaboratorio estado, Boolean activo, Pageable pageable) {

        Specification<Laboratorio> specification =
                LaboratorioSpecification.nombreOCodigoContiene(texto)
                        .and(LaboratorioSpecification.tieneEstadoLaboratorio(estado))
                        .and(LaboratorioSpecification.tieneEstado(activo));

        return laboratorioRepository.findAll(specification, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LaboratorioResponse> listarDisponibles() {
        return laboratorioRepository.findByEstadoAndActivoTrue(EstadoLaboratorio.DISPONIBLE)
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

        laboratorio.setPisoId(request.pisoId());
        laboratorio.setCodigo(request.codigo());
        laboratorio.setNombre(request.nombre());
        laboratorio.setCapacidad(request.capacidad());
        laboratorio.setDescripcion(request.descripcion());

        Laboratorio actualizado = laboratorioRepository.save(laboratorio);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional
    public LaboratorioResponse cambiarEstado(UUID id, EstadoLaboratorio estado) {

        Laboratorio laboratorio = buscarLaboratorio(id);
        laboratorio.setEstado(estado);

        // Si se marca INACTIVO, también se desactiva lógicamente
        if (estado == EstadoLaboratorio.INACTIVO) {
            laboratorio.setActivo(false);
        } else if (!laboratorio.isActivo()) {
            laboratorio.setActivo(true);
        }

        Laboratorio actualizado = laboratorioRepository.save(laboratorio);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public LaboratorioDisponibilidadBaseResponse obtenerDisponibilidadBase(UUID id) {

        return laboratorioRepository.findById(id)
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
        boolean existe = laboratorioRepository.existsById(id);
        return new ExisteResponse(id, existe);
    }

    private Laboratorio buscarLaboratorio(UUID id) {
        return laboratorioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un laboratorio con el id: " + id));
    }

    private void validarPisoExiste(UUID pisoId) {
        if (!pisoRepository.existsById(pisoId)) {
            throw new BusinessRuleException(
                    "No existe un piso con el id: " + pisoId);
        }
    }

    private void validarCodigoDuplicado(String codigo, UUID idActual) {

        boolean existe = (idActual == null)
                ? laboratorioRepository.existsByCodigo(codigo)
                : laboratorioRepository.existsByCodigoAndIdNot(codigo, idActual);

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
