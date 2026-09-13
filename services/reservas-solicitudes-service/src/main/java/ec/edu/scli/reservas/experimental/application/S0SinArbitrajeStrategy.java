package ec.edu.scli.reservas.experimental.application;

import ec.edu.scli.reservas.experimental.domain.*;
import ec.edu.scli.reservas.experimental.port.ExperimentalAllocationStore;

public final class S0SinArbitrajeStrategy implements ArbitrajeStrategy {
    private final ExperimentalAllocationStore store;
    public S0SinArbitrajeStrategy(ExperimentalAllocationStore store) { this.store = store; }
    public String nombre() { return "s0"; }
    public ResultadoArbitraje adjudicar(SolicitudArbitraje solicitud) { return store.directa(solicitud, nombre()); }
}
