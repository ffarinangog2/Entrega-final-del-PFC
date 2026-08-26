package ec.edu.uteq.scli.api_gateway;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import ec.edu.uteq.scli.api_gateway.config.JwtAuthenticationEntryPoint;
import ec.edu.uteq.scli.api_gateway.config.SecurityConfig;

@WebMvcTest
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class GatewaySecurityIntegrationTests {

    private static final byte[] TEST_KEY = "0123456789abcdef0123456789abcdef".getBytes();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginSinTokenEstaPermitido() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(status().isNotFound());
    }

    @Test
    void refreshSinTokenEstaPermitido() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isNotFound());
    }

    @Test
    void recuperacionPostSinTokenEstaPermitida() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/auth/reset-password")).andExpect(status().isNotFound());
    }

    @Test
    void recuperacionGetSigueProtegida() throws Exception {
        mockMvc.perform(get("/api/v1/auth/forgot-password")).andExpect(status().isUnauthorized());
    }

    @Test
    void preflightEstaPermitido() throws Exception {
        mockMvc.perform(options("/api/v1/usuarios/prueba")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk());
    }

    @Test
    void endpointProtegidoSinTokenDevuelveJson401() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/prueba"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/v1/usuarios/prueba"));
    }

    @Test
    void bearerMalformadoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/prueba")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoDevuelve401() throws Exception {
        String token = token("scli-auth-service", "access", Instant.now().minusSeconds(120), TEST_KEY);
        mockMvc.perform(get("/api/v1/usuarios/prueba").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenNoSeAceptaComoAccess() throws Exception {
        String token = token("scli-auth-service", "refresh", Instant.now().plusSeconds(120), TEST_KEY);
        mockMvc.perform(get("/api/v1/usuarios/prueba").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenValidoPasaLaCapaDeSeguridad() throws Exception {
        String token = token("scli-auth-service", "access", Instant.now().plusSeconds(120), TEST_KEY);
        mockMvc.perform(get("/api/v1/usuarios/prueba").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void issuerIncorrectoDevuelve401() throws Exception {
        String token = token("otro-issuer", "access", Instant.now().plusSeconds(120), TEST_KEY);
        mockMvc.perform(get("/api/v1/usuarios/prueba").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void firmaInvalidaDevuelve401() throws Exception {
        byte[] otherKey = "abcdef0123456789abcdef0123456789".getBytes();
        String token = token("scli-auth-service", "access", Instant.now().plusSeconds(120), otherKey);
        mockMvc.perform(get("/api/v1/usuarios/prueba").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    private String token(String issuer, String type, Instant expiresAt, byte[] keyBytes) {
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("usuario-test")
                .issuedAt(expiresAt.minusSeconds(60))
                .expiresAt(expiresAt)
                .claim("type", type)
                .build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

}
