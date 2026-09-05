package ec.edu.scli.academico.domain.port;

import ec.edu.scli.academico.domain.model.Piso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PisoRepositoryPort {

    Piso guardar(Piso piso);

    Optional<Piso> buscarPorId(UUID id);

    Page<Piso> buscar(UUID bloqueId, Boolean activo, Pageable pageable);

    List<Piso> buscarPorBloque(UUID bloqueId);

    boolean existeNumeroEnBloque(UUID bloqueId, Integer numero);

    boolean existeNumeroEnBloqueParaOtroId(UUID bloqueId, Integer numero, UUID id);
}
