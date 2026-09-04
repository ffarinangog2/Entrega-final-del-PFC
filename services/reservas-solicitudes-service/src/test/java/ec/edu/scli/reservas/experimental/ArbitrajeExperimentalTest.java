package ec.edu.scli.reservas.experimental;

import ec.edu.scli.reservas.experimental.application.*;
import ec.edu.scli.reservas.experimental.domain.*;
import ec.edu.scli.reservas.experimental.port.ExperimentalAllocationStore;
import ec.edu.scli.reservas.experimental.presentation.ExperimentalArbiterController;
import ec.edu.scli.reservas.experimental.presentation.AdjudicacionExperimentalRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArbitrajeExperimentalTest {
    private final SolicitudArbitraje solicitud = new SolicitudArbitraje("run", "req", UUID.randomUUID(),
            UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-09-07T08:00:00Z"),
            Instant.parse("2026-09-07T10:00:00Z"));

    @Test
    void configuracionAusenteNoActivaSubsistemaExperimental() {
        new ApplicationContextRunner()
                .withUserConfiguration(ec.edu.scli.reservas.experimental.config.ExperimentalArbiterConfig.class)
                .run(context -> assertFalse(context.containsBean("resolver")));
        new ApplicationContextRunner()
                .withUserConfiguration(ExperimentalArbiterController.class)
                .run(context -> assertFalse(context.containsBean("experimentalArbiterController")));
    }

    @Test
    void resolverSeleccionaCadaEstrategiaSinDefaultImplicito() {
        ExperimentalAllocationStore store = mock(ExperimentalAllocationStore.class);
        var strategies = List.<ArbitrajeStrategy>of(new S0SinArbitrajeStrategy(store),
                new S1OptimistaStrategy(store), new S2PesimistaStrategy(store),
                new S3BullyLamportStrategy(store, cluster()), new S4SerializableQuorumStrategy(store));
        for (String name : List.of("s0", "s1", "s2", "s3", "s4")) {
            assertEquals(name, new ArbitrajeStrategyResolver(strategies, name).resolve().nombre());
        }
        assertThrows(IllegalArgumentException.class, () -> new ArbitrajeStrategyResolver(strategies, ""));
    }

    @Test
    void estrategiasDeleganEnLaOperacionConcurrenteCorrespondiente() {
        ExperimentalAllocationStore store = mock(ExperimentalAllocationStore.class);
        new S0SinArbitrajeStrategy(store).adjudicar(solicitud);
        new S1OptimistaStrategy(store).adjudicar(solicitud);
        new S2PesimistaStrategy(store).adjudicar(solicitud);
        new S4SerializableQuorumStrategy(store).adjudicar(solicitud);
        verify(store).directa(solicitud, "s0");
        verify(store).optimista(solicitud, "s1");
        verify(store).pesimista(solicitud, "s2");
        verify(store).serializable(solicitud, "s4");
    }

    @Test
    void s3OrdenaConLamportYReeligePorBully() {
        ExperimentalAllocationStore store = mock(ExperimentalAllocationStore.class);
        when(store.pesimista(solicitud, "s3")).thenReturn(resultado("s3"));
        var cluster = cluster();
        var result = new S3BullyLamportStrategy(store, cluster).adjudicar(solicitud);
        assertEquals(3, result.leaderId());
        assertTrue(result.lamport() > 0);
        cluster.failLeader();
        assertEquals(2, cluster.leaderId());
    }

    @Test
    void bullyRegistraHeartbeatEventosYDetectaNodoVencido() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-07T10:00:00Z"), ZoneOffset.UTC);
        var cluster = new BullyCluster(new LamportClock(), clock, 1, 2, 3);
        cluster.heartbeat(2);
        assertEquals(List.of(), cluster.detectFailures(Duration.ofMinutes(1)));
        assertFalse(cluster.events().isEmpty());
        assertThrows(IllegalStateException.class, () -> cluster.heartbeat(99));
    }

    @Test
    void equipoNoOperativoSeRechazaAntesDeAdjudicar() {
        ArbitrajeStrategy strategy = mock(ArbitrajeStrategy.class);
        when(strategy.nombre()).thenReturn("s1");
        var service = new ExperimentalArbiterService(new ArbitrajeStrategyResolver(List.of(strategy), "s1"));
        assertEquals("EQUIPMENT_UNAVAILABLE", service.adjudicar(solicitud, "MANTENIMIENTO", true).motivo());
        verify(strategy, never()).adjudicar(any());
    }

    @Test
    void equipoOperativoInvocaLaEstrategiaSeleccionada() {
        ArbitrajeStrategy strategy = mock(ArbitrajeStrategy.class);
        when(strategy.nombre()).thenReturn("s2");
        when(strategy.adjudicar(solicitud)).thenReturn(resultado("s2"));
        var service = new ExperimentalArbiterService(new ArbitrajeStrategyResolver(List.of(strategy), " S2 "));
        assertEquals("CONFIRMED", service.adjudicar(solicitud, "OPERATIVO", true).estado());
        verify(strategy).adjudicar(solicitud);
    }

    @Test
    void endpointExigeApiKeyInterna() {
        var controller = new ExperimentalArbiterController(mock(ExperimentalArbiterService.class), "secret");
        assertThrows(ResponseStatusException.class, () -> controller.fallarLider("incorrecta"));
    }

    @Test
    void endpointValidoDelegaYFalloLiderSoloAplicaAS3() {
        var service = mock(ExperimentalArbiterService.class);
        when(service.adjudicar(any(), eq("OPERATIVO"), eq(true))).thenReturn(resultado("s1"));
        when(service.selected()).thenReturn(mock(S1OptimistaStrategy.class));
        var controller = new ExperimentalArbiterController(service, "secret");
        var request = new AdjudicacionExperimentalRequest(solicitud.runId(), solicitud.requestId(),
                solicitud.equipmentId(), solicitud.laboratorioId(), solicitud.agenteId(), solicitud.inicio(),
                solicitud.fin(), "OPERATIVO", true, "fixture");
        assertEquals("CONFIRMED", controller.adjudicar("secret", request).getBody().estado());
        assertEquals(409, controller.fallarLider("secret").getStatusCode().value());
    }

    @Test
    void endpointS3ProvocaReeleccionDelLiderBackend() {
        ExperimentalAllocationStore store = mock(ExperimentalAllocationStore.class);
        var s3 = new S3BullyLamportStrategy(store, cluster());
        var service = new ExperimentalArbiterService(new ArbitrajeStrategyResolver(List.of(s3), "s3"));
        var body = new ExperimentalArbiterController(service, "secret").fallarLider("secret").getBody();
        assertNotNull(body);
        assertEquals(3, body.get("previousLeaderId"));
        assertEquals(2, body.get("leaderId"));
    }

    @Test
    void solicitudRechazaFranjaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new SolicitudArbitraje("run", "req",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), solicitud.fin(), solicitud.inicio()));
    }

    private BullyCluster cluster() {
        return new BullyCluster(new LamportClock(), Clock.systemUTC(), 1, 2, 3);
    }

    private ResultadoArbitraje resultado(String strategy) {
        return new ResultadoArbitraje("run", "req", strategy, "CONFIRMED", null, 0,
                null, null, null, Instant.now());
    }
}
