package ec.edu.scli.reservas.client.dto;

import java.util.UUID;

public record MateriaContextoExternoResponse(UUID id, UUID carreraId, Integer nivel, boolean existe, boolean activo) {
    public MateriaContextoExternoResponse(UUID id, UUID carreraId, boolean existe, boolean activo) {
        this(id, carreraId, null, existe, activo);
    }
}
