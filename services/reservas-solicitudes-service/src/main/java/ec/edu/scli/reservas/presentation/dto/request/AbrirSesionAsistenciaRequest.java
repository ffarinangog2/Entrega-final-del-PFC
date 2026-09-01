package ec.edu.scli.reservas.presentation.dto.request;
import jakarta.validation.constraints.NotNull; import java.util.UUID;
public record AbrirSesionAsistenciaRequest(@NotNull UUID reservaId) {}
