package ec.edu.scli.academico.domain.port;

import ec.edu.scli.academico.domain.model.Equipo;
import ec.edu.scli.academico.enums.EstadoEquipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EquipoRepositoryPort {

    Equipo guardar(Equipo equipo);

    Optional<Equipo> buscarPorId(UUID id);

    Page<Equipo> buscar(UUID laboratorioId, EstadoEquipo estado, Boolean activo, Pageable pageable);

    List<Equipo> buscarPorLaboratorio(UUID laboratorioId);

    boolean existeCodigoInventario(String codigoInventario);

    boolean existeCodigoInventarioParaOtroId(String codigoInventario, UUID id);

    boolean existeNumeroSerie(String numeroSerie);

    boolean existeNumeroSerieParaOtroId(String numeroSerie, UUID id);
}
