package ec.edu.scli.reservas.domain.state.solicitud;

import ec.edu.scli.reservas.domain.model.EstadoSolicitud;

import java.util.Map;
import java.util.Set;

public final class PendienteState extends AbstractSolicitudReservaState {
    public PendienteState() {
        super(Map.of(AccionSolicitud.PONER_EN_REVISION, EstadoSolicitud.EN_REVISION),
                Set.of(AccionSolicitud.ACTUALIZAR));
    }
}
