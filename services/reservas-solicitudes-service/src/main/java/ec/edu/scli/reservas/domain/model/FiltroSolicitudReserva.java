package ec.edu.scli.reservas.domain.model;

import java.time.LocalDate;
import java.util.UUID;

public record FiltroSolicitudReserva(EstadoSolicitud estado, UUID solicitanteId,
                                      UUID laboratorioId, LocalDate fecha) { }
