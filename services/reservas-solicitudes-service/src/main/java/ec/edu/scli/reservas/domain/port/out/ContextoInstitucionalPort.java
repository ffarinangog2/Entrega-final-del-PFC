package ec.edu.scli.reservas.domain.port.out;

import ec.edu.scli.reservas.domain.model.ContextoInstitucional;
import java.util.UUID;

public interface ContextoInstitucionalPort {
    ContextoInstitucional obtenerPorPerfilId(UUID perfilId);
}
