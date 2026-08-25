package ec.edu.scli.reservas.domain.model;

import java.util.UUID;

public record ContextoInstitucional(
        boolean perfilExiste, boolean perfilActivo,
        boolean administradorExiste, boolean administradorActivo,
        boolean administradorPisoOperativo, UUID pisoId) { }
