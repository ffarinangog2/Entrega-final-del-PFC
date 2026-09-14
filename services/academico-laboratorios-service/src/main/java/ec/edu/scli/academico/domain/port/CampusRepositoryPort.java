package ec.edu.scli.academico.domain.port;

import ec.edu.scli.academico.domain.model.Campus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida del dominio para Campus (patrón Repository).
 * La capa application depende únicamente de esta interfaz; quien la
 * implementa (infrastructure.persistence.adapter.CampusRepositoryAdapter)
 * es el único punto que conoce Spring Data / JPA.
 *
 * Nota pragmática: se usan Page/Pageable de Spring Data como estructura
 * de paginación neutral en vez de reinventar una propia; es un compromiso
 * aceptado en arquitectura hexagonal con Spring, documentado en ADR-005.
 */
public interface CampusRepositoryPort {

    Campus guardar(Campus campus);

    Optional<Campus> buscarPorId(UUID id);

    Page<Campus> buscar(String codigo, String nombre, Boolean activo, Pageable pageable);

    boolean existeCodigo(String codigo);

    boolean existeCodigoParaOtroId(String codigo, UUID id);
}
