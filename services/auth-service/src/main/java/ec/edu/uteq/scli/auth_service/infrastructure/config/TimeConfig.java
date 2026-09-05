package ec.edu.uteq.scli.auth_service.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class TimeConfig {
    @Bean public Clock clock() { return Clock.systemUTC(); }
}
