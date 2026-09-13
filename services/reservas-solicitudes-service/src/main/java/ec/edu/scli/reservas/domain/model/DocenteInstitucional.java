package ec.edu.scli.reservas.domain.model;

import java.util.UUID;

public record DocenteInstitucional(UUID docenteId, UUID perfilId, boolean activo) { }
