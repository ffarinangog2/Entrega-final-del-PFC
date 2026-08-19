package ec.edu.scli.academico.service;

import ec.edu.scli.academico.dto.internal.ExisteResponse;
import ec.edu.scli.academico.dto.materia.MateriaRequest;
import ec.edu.scli.academico.dto.materia.MateriaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface MateriaService {

    MateriaResponse crear(MateriaRequest request);

    Page<MateriaResponse> listar(UUID carreraId, String codigo, String nombre, Boolean activo, Pageable pageable);

    List<MateriaResponse> listarPorCarrera(UUID carreraId);

    MateriaResponse obtenerPorId(UUID id);

    MateriaResponse actualizar(UUID id, MateriaRequest request);

    void eliminar(UUID id);

    ExisteResponse verificarExistencia(UUID id);
}
