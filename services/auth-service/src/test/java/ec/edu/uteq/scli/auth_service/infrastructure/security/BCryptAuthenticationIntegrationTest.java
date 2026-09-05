package ec.edu.uteq.scli.auth_service.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

class BCryptAuthenticationIntegrationTest {
    @Test
    void daoAuthenticationProviderComparaPasswordRealContraBcryptCosteDoce() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String hash = encoder.encode("ClaveIntegracion1!");
        UserDetailsService users = username -> User.withUsername(username).password(hash).authorities("LOGIN").build();
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);

        var authenticated = provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("usuario", "ClaveIntegracion1!"));

        assertTrue(authenticated.isAuthenticated());
        assertTrue(encoder.matches("ClaveIntegracion1!", hash));
        assertTrue(hash.startsWith("$2"));
        assertEquals("12", hash.substring(4, 6));
    }
}
