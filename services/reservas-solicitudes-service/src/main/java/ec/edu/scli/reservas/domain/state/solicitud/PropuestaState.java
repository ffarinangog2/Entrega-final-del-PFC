package ec.edu.scli.reservas.domain.state.solicitud;

import ec.edu.scli.reservas.domain.model.EstadoSolicitud;
import java.util.Map;
import java.util.Set;

public final class PropuestaState extends AbstractSolicitudReservaState {
    public PropuestaState() {
        super(Map.of(
                AccionSolicitud.ACEPTAR_PROPUESTA, EstadoSolicitud.EN_REVISION,
                AccionSolicitud.RECHAZAR_PROPUESTA, EstadoSolicitud.EN_REVISION,
                AccionSolicitud.CANCELAR, EstadoSolicitud.CANCELADA), Set.of());
    }
}
