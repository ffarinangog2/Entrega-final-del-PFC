package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.application.service.MateriaService;
import ec.edu.scli.academico.domain.model.Materia;
import ec.edu.scli.academico.domain.port.CarreraRepositoryPort;
import ec.edu.scli.academico.domain.port.MateriaRepositoryPort;
import ec.edu.scli.academico.dto.internal.ExisteResponse;
import ec.edu.scli.academico.exception.BusinessRuleException;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.materia.MateriaRequest;
import ec.edu.scli.academico.presentation.dto.materia.MateriaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MateriaServiceImpl implements MateriaService {

    private final MateriaRepositoryPort materiaRepositoryPort;
    private final CarreraRepositoryPort carreraRepositoryPort;

    public MateriaServiceImpl(
            MateriaRepositoryPort materiaRepositoryPort,
            CarreraRepositoryPort carreraRepositoryPort
    ) {
        this.materiaRepositoryPort = materiaRepositoryPort;
        this.carreraRepositoryPort = carreraRepositoryPort;
    }

    @Override
    @Transactional
    public MateriaResponse crear(MateriaRequest request) {

        validarCarreraExiste(request.carreraId());
        validarCodigoDuplicado(request.codigo(), null);

        Materia materia = Materia.nueva(
                request.carreraId(),
                request.codigo(),
                request.nombre(),
                request.numeroHoras()
        );

        Materia guardada = materiaRepositoryPort.guardar(materia);

        return convertirAResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MateriaResponse> listar(UUID carreraId, String codigo, String nombre, Boolean activo, Pageable pageable) {
        return materiaRepositoryPort.buscar(carreraId, codigo, nombre, activo, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MateriaResponse> listarPorCarrera(UUID carreraId) {

        validarCarreraExiste(carreraId);

        return materiaRepositoryPort.buscarPorCarrera(carreraId)
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

        materia.actualizarDatos(
                request.carreraId(),
                request.codigo(),
                request.nombre(),
                request.numeroHoras()
        );

        Materia actualizada = materiaRepositoryPort.guardar(materia);

        return convertirAResponse(actualizada);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {

        Materia materia = buscarMateria(id);
        materia.desactivar();

        materiaRepositoryPort.guardar(materia);
    }

    @Override
    @Transactional(readOnly = true)
    public ExisteResponse verificarExistencia(UUID id) {
        return new ExisteResponse(id, materiaRepositoryPort.existePorId(id));
    }

    private Materia buscarMateria(UUID id) {
        return materiaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una materia con el id: " + id));
    }

    private void validarCarreraExiste(UUID carreraId) {
        if (carreraRepositoryPort.buscarPorId(carreraId).isEmpty()) {
            throw new BusinessRuleException(
                    "No existe una carrera con el id: " + carreraId);
        }
    }

    private void validarCodigoDuplicado(String codigo, UUID idActual) {

        boolean existe = (idActual == null)
                ? materiaRepositoryPort.existeCodigo(codigo)
                : materiaRepositoryPort.existeCodigoParaOtroId(codigo, idActual);

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
