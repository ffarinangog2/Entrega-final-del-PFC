package ec.edu.scli.reservas.domain.port.out;

import ec.edu.scli.reservas.domain.model.DocenteInstitucional;
import java.util.UUID;

public interface DocenteInstitucionalPort {
    DocenteInstitucional obtenerPorPerfilId(UUID perfilId);
    DocenteInstitucional obtenerPorDocenteId(UUID docenteId);
}
