package ec.edu.uteq.scli.auth_service.infrastructure.audit;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class AuthenticationEventPublisherConfigTest {

    @Test
    void exponeUnDefaultAuthenticationEventPublisher() {
        AuthenticationEventPublisherConfig config = new AuthenticationEventPublisherConfig();
        ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);

        AuthenticationEventPublisher publisher = config.authenticationEventPublisher(applicationEventPublisher);

        assertInstanceOf(DefaultAuthenticationEventPublisher.class, publisher);
    }
}
