package ec.edu.scli.reservas.client.dto;

import java.util.UUID;

public record DocenteExternoResponse(UUID docenteId, UUID perfilId, boolean activo) { }
