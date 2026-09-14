package ec.edu.scli.reservas.domain.state.solicitud;

import ec.edu.scli.reservas.domain.model.EstadoSolicitud;

import java.util.Map;
import java.util.Set;

public final class AprobadaState extends AbstractSolicitudReservaState {
    public AprobadaState() {
        super(Map.of(AccionSolicitud.CANCELAR, EstadoSolicitud.CANCELADA), Set.of());
    }
}
