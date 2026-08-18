package ec.edu.scli.reservas.domain.state.solicitud;

import ec.edu.scli.reservas.domain.model.EstadoSolicitud;

import java.util.Map;
import java.util.Set;

public final class EnRevisionState extends AbstractSolicitudReservaState {
    public EnRevisionState() {
        super(Map.of(
                AccionSolicitud.APROBAR, EstadoSolicitud.APROBADA,
                AccionSolicitud.RECHAZAR, EstadoSolicitud.RECHAZADA),
                Set.of(AccionSolicitud.ACTUALIZAR));
    }
}
