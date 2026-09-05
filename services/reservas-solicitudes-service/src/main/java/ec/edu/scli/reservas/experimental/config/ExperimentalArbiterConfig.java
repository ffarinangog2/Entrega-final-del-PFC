package ec.edu.scli.reservas.experimental.config;

import ec.edu.scli.reservas.experimental.application.*;
import ec.edu.scli.reservas.experimental.domain.ArbitrajeStrategy;
import ec.edu.scli.reservas.experimental.port.ExperimentalAllocationStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

@Configuration
@ConditionalOnProperty(name = "app.experimental.arbiter.enabled", havingValue = "true")
public class ExperimentalArbiterConfig {
    @Bean BullyCluster bullyCluster() { return new BullyCluster(new LamportClock(), Clock.systemUTC(), 1, 2, 3); }
    @Bean S0SinArbitrajeStrategy s0(ExperimentalAllocationStore store) { return new S0SinArbitrajeStrategy(store); }
    @Bean S1OptimistaStrategy s1(ExperimentalAllocationStore store) { return new S1OptimistaStrategy(store); }
    @Bean S2PesimistaStrategy s2(ExperimentalAllocationStore store) { return new S2PesimistaStrategy(store); }
    @Bean S3BullyLamportStrategy s3(ExperimentalAllocationStore store, BullyCluster cluster) { return new S3BullyLamportStrategy(store, cluster); }
    @Bean S4SerializableQuorumStrategy s4(ExperimentalAllocationStore store) { return new S4SerializableQuorumStrategy(store); }
    @Bean ArbitrajeStrategyResolver resolver(List<ArbitrajeStrategy> strategies,
            @Value("${app.experimental.arbiter.strategy:}") String strategy) {
        return new ArbitrajeStrategyResolver(strategies, strategy);
    }
    @Bean ExperimentalArbiterService experimentalArbiterService(ArbitrajeStrategyResolver resolver) {
        return new ExperimentalArbiterService(resolver);
    }
}
