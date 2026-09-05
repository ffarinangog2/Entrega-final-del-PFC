package ec.edu.scli.reservas.domain.model;

import java.time.LocalDate;
import java.util.UUID;

public record FiltroReserva(EstadoReserva estado, UUID laboratorioId, UUID responsableId,
                            UUID pisoId, UUID solicitanteId,
                            LocalDate fechaDesde, LocalDate fechaHasta) { }
