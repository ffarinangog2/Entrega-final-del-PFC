package ec.edu.scli.reservas.presentation.dto.response;
import java.util.List;
import java.util.UUID;
public record DisponibilidadPlanificacionResponse(List<UUID> docentesOcupados, List<UUID> laboratoriosOcupados) { }
