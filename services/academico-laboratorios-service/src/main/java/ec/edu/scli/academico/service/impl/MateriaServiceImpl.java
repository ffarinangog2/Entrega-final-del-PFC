package ec.edu.scli.academico.service.impl;

import ec.edu.scli.academico.dto.internal.ExisteResponse;
import ec.edu.scli.academico.dto.materia.MateriaRequest;
import ec.edu.scli.academico.dto.materia.MateriaResponse;
import ec.edu.scli.academico.entity.Materia;
import ec.edu.scli.academico.exception.BusinessRuleException;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.repository.CarreraRepository;
import ec.edu.scli.academico.repository.MateriaRepository;
import ec.edu.scli.academico.service.MateriaService;
import ec.edu.scli.academico.specification.MateriaSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MateriaServiceImpl implements MateriaService {

    private final MateriaRepository materiaRepository;
    private final CarreraRepository carreraRepository;

    public MateriaServiceImpl(MateriaRepository materiaRepository, CarreraRepository carreraRepository) {
        this.materiaRepository = materiaRepository;
        this.carreraRepository = carreraRepository;
    }

    @Override
    @Transactional
    public MateriaResponse crear(MateriaRequest request) {

        validarCarreraExiste(request.carreraId());
        validarCodigoDuplicado(request.codigo(), null);

        Materia materia = new Materia();
        materia.setCarreraId(request.carreraId());
        materia.setCodigo(request.codigo());
        materia.setNombre(request.nombre());
        materia.setNumeroHoras(request.numeroHoras());
        materia.setActivo(true);

        Materia guardada = materiaRepository.save(materia);

        return convertirAResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MateriaResponse> listar(UUID carreraId, String codigo, String nombre, Boolean activo, Pageable pageable) {

        Specification<Materia> specification =
                MateriaSpecification.tieneCarrera(carreraId)
                        .and(MateriaSpecification.codigoContiene(codigo))
                        .and(MateriaSpecification.nombreContiene(nombre))
                        .and(MateriaSpecification.tieneEstado(activo));

        return materiaRepository.findAll(specification, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MateriaResponse> listarPorCarrera(UUID carreraId) {

        validarCarreraExiste(carreraId);

        return materiaRepository.findByCarreraId(carreraId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MateriaResponse obtenerPorId(UUID id) {
        return convertirAResponse(buscarMateria(id));
    }

    @Override
    @Transactional
    public MateriaResponse actualizar(UUID id, MateriaRequest request) {

        Materia materia = buscarMateria(id);

        validarCarreraExiste(request.carreraId());
        validarCodigoDuplicado(request.codigo(), id);

        materia.setCarreraId(request.carreraId());
        materia.setCodigo(request.codigo());
        materia.setNombre(request.nombre());
        materia.setNumeroHoras(request.numeroHoras());

        Materia actualizada = materiaRepository.save(materia);

        return convertirAResponse(actualizada);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {

        Materia materia = buscarMateria(id);
        materia.setActivo(false);

        materiaRepository.save(materia);
    }

    @Override
    @Transactional(readOnly = true)
    public ExisteResponse verificarExistencia(UUID id) {
        boolean existe = materiaRepository.existsById(id);
        return new ExisteResponse(id, existe);
    }

    private Materia buscarMateria(UUID id) {
        return materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una materia con el id: " + id));
    }

    private void validarCarreraExiste(UUID carreraId) {
        if (!carreraRepository.existsById(carreraId)) {
            throw new BusinessRuleException(
                    "No existe una carrera con el id: " + carreraId);
        }
    }

    private void validarCodigoDuplicado(String codigo, UUID idActual) {

        boolean existe = (idActual == null)
                ? materiaRepository.existsByCodigo(codigo)
                : materiaRepository.existsByCodigoAndIdNot(codigo, idActual);

        if (existe) {
            throw new ConflictException("Ya existe una materia con el código: " + codigo);
        }
    }

    private MateriaResponse convertirAResponse(Materia materia) {
        return new MateriaResponse(
                materia.getId(),
                materia.getCarreraId(),
                materia.getCodigo(),
                materia.getNombre(),
                materia.getNumeroHoras(),
                materia.isActivo(),
                materia.getCreadoEn(),
                materia.getActualizadoEn()
        );
    }
}
