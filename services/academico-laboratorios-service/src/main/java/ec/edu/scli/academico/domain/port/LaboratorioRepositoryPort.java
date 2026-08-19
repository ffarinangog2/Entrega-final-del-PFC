package ec.edu.scli.academico.domain.port;

import ec.edu.scli.academico.domain.model.Laboratorio;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LaboratorioRepositoryPort {

    Laboratorio guardar(Laboratorio laboratorio);

    Optional<Laboratorio> buscarPorId(UUID id);

    Page<Laboratorio> buscar(String texto, EstadoLaboratorio estado, Boolean activo, Pageable pageable);

    List<Laboratorio> buscarDisponibles();

    boolean existeCodigo(String codigo);

    boolean existeCodigoParaOtroId(String codigo, UUID id);

    boolean existePorId(UUID id);
}
