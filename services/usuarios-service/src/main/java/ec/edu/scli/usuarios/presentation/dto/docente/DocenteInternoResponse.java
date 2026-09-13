package ec.edu.scli.usuarios.presentation.dto.docente;

import java.util.UUID;

public record DocenteInternoResponse(UUID docenteId, UUID perfilId, boolean activo) { }
