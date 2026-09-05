package ec.edu.uteq.scli.auth_service.infrastructure.audit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;

/**
 * Habilita que el {@code AuthenticationManager} (ver SecurityConfig) publique
 * AuthenticationSuccessEvent / AbstractAuthenticationFailureEvent en cada
 * authenticationManager.authenticate(), sin requerir cambios en AuthService
 * ni en SecurityConfig.
 */
@Configuration
public class AuthenticationEventPublisherConfig {

    @Bean
    public AuthenticationEventPublisher authenticationEventPublisher(
            ApplicationEventPublisher applicationEventPublisher) {
        return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
    }
}
