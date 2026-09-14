package ec.edu.scli.academico.domain.port;

import ec.edu.scli.academico.domain.model.TipoEquipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface TipoEquipoRepositoryPort {

    TipoEquipo guardar(TipoEquipo tipoEquipo);

    Optional<TipoEquipo> buscarPorId(UUID id);

    Page<TipoEquipo> buscar(String codigo, String nombre, Boolean activo, Pageable pageable);

    boolean existeCodigo(String codigo);

    boolean existeCodigoParaOtroId(String codigo, UUID id);

    boolean existePorId(UUID id);
}
