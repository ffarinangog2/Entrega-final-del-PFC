package ec.edu.scli.reservas.experimental.application;

import ec.edu.scli.reservas.experimental.domain.*;
import ec.edu.scli.reservas.experimental.port.ExperimentalAllocationStore;

public final class S2PesimistaStrategy implements ArbitrajeStrategy {
    private final ExperimentalAllocationStore store;
    public S2PesimistaStrategy(ExperimentalAllocationStore store) { this.store = store; }
    public String nombre() { return "s2"; }
    public ResultadoArbitraje adjudicar(SolicitudArbitraje solicitud) { return store.pesimista(solicitud, nombre()); }
}
