package ec.edu.scli.academico.domain.port;

import ec.edu.scli.academico.domain.model.Bloque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BloqueRepositoryPort {

    Bloque guardar(Bloque bloque);

    Optional<Bloque> buscarPorId(UUID id);

    Page<Bloque> buscar(UUID campusId, String nombre, Boolean activo, Pageable pageable);

    List<Bloque> buscarPorCampus(UUID campusId);

    boolean existeCodigoEnCampus(UUID campusId, String codigo);

    boolean existeCodigoEnCampusParaOtroId(UUID campusId, String codigo, UUID id);

    boolean existePorId(UUID id);
}
