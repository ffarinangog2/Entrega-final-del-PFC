package ec.edu.scli.reservas.experimental.application;

import ec.edu.scli.reservas.experimental.domain.*;
import ec.edu.scli.reservas.experimental.port.ExperimentalAllocationStore;

import java.time.Instant;

public final class S3BullyLamportStrategy implements ArbitrajeStrategy {
    private final ExperimentalAllocationStore store;
    private final BullyCluster cluster;
    public S3BullyLamportStrategy(ExperimentalAllocationStore store, BullyCluster cluster) {
        this.store = store; this.cluster = cluster;
    }
    public String nombre() { return "s3"; }
    public ResultadoArbitraje adjudicar(SolicitudArbitraje solicitud) {
        try (var ordered = cluster.acquireOrder()) {
            ResultadoArbitraje base = store.pesimista(solicitud, nombre());
            return new ResultadoArbitraje(base.runId(), base.requestId(), nombre(), base.estado(), base.motivo(),
                    base.version(), ordered.leaderId(), ordered.leaderId(), ordered.lamport(), Instant.now());
        }
    }
    public long fallarLider() { return cluster.failLeader(); }
    public BullyCluster cluster() { return cluster; }
}
