package ec.edu.scli.reservas.experimental.application;

import ec.edu.scli.reservas.experimental.domain.*;
import ec.edu.scli.reservas.experimental.port.ExperimentalAllocationStore;

public final class S1OptimistaStrategy implements ArbitrajeStrategy {
    private final ExperimentalAllocationStore store;
    public S1OptimistaStrategy(ExperimentalAllocationStore store) { this.store = store; }
    public String nombre() { return "s1"; }
    public ResultadoArbitraje adjudicar(SolicitudArbitraje solicitud) { return store.optimista(solicitud, nombre()); }
}
