package ec.edu.scli.reservas.experimental.application;

import ec.edu.scli.reservas.experimental.domain.*;
import ec.edu.scli.reservas.experimental.port.ExperimentalAllocationStore;

public final class S4SerializableQuorumStrategy implements ArbitrajeStrategy {
    private final ExperimentalAllocationStore store;
    public S4SerializableQuorumStrategy(ExperimentalAllocationStore store) { this.store = store; }
    public String nombre() { return "s4"; }
    public ResultadoArbitraje adjudicar(SolicitudArbitraje solicitud) { return store.serializable(solicitud, nombre()); }
}
