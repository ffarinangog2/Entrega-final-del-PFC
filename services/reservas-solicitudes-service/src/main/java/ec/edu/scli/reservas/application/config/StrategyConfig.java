package ec.edu.scli.reservas.application.config;

import ec.edu.scli.reservas.domain.strategy.disponibilidad.DisponibilidadSinConflictosStrategy;
import ec.edu.scli.reservas.domain.strategy.disponibilidad.DisponibilidadStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StrategyConfig {
    @Bean
    DisponibilidadStrategy disponibilidadStrategy() {
        return new DisponibilidadSinConflictosStrategy();
    }
}
