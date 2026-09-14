package ec.edu.scli.reservas.domain.state.solicitud;

import java.util.Map;
import java.util.Set;

public final class RechazadaState extends AbstractSolicitudReservaState {
    public RechazadaState() { super(Map.of(), Set.of()); }
}
