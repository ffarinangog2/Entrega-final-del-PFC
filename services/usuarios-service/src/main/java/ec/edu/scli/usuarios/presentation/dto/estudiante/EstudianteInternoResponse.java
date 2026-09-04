package ec.edu.scli.usuarios.presentation.dto.estudiante;

import java.util.UUID;

public record EstudianteInternoResponse(UUID estudianteId, UUID perfilId, UUID carreraId,
        UUID periodoId, Integer nivel, boolean activo) { }
