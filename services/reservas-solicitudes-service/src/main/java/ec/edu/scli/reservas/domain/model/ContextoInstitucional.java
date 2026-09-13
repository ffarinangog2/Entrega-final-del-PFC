package ec.edu.scli.reservas.domain.model;

import java.util.List;
import java.util.UUID;

public record ContextoInstitucional(
        boolean perfilExiste, boolean perfilActivo,
        boolean administradorExiste, boolean administradorActivo,
        boolean administradorPisoOperativo, UUID pisoId, List<UUID> carreraIds) {
    public ContextoInstitucional {
        carreraIds = carreraIds == null ? List.of() : List.copyOf(carreraIds);
    }
    public ContextoInstitucional(boolean perfilExiste, boolean perfilActivo,
            boolean administradorExiste, boolean administradorActivo,
            boolean administradorPisoOperativo, UUID pisoId) {
        this(perfilExiste, perfilActivo, administradorExiste, administradorActivo,
                administradorPisoOperativo, pisoId, List.of());
    }
}
