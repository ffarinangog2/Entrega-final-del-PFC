package ec.edu.scli.academico.dto.internal;

import java.util.UUID;

public record MateriaContextoResponse(UUID id, UUID carreraId, Integer nivel, boolean existe, boolean activo) { }
