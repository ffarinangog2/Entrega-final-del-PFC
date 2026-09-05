package ec.edu.scli.reservas.presentation.dto.request;
import ec.edu.scli.reservas.domain.model.TipoSolicitudCambio; import jakarta.validation.constraints.*; import java.time.LocalTime; import java.util.UUID;
public record CrearSolicitudCambioRequest(@NotNull UUID bloqueId,@NotNull TipoSolicitudCambio tipo,@NotBlank String motivo,UUID laboratorioId,UUID docenteId,String diaSemana,LocalTime horaInicio,LocalTime horaFin){}
