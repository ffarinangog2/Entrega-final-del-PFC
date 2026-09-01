package ec.edu.scli.reservas.client.dto;
import java.util.UUID;
public record EstudianteExternoResponse(UUID estudianteId, UUID perfilId, UUID carreraId, boolean activo) { }
