package ec.edu.scli.contract.auth;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "auth-service")
class AuthConsumerPactTest {

    private static final String USUARIO_ID = "11111111-1111-1111-1111-111111111111";
    private static final String PERFIL_ID = "a0000000-0000-0000-0000-000000000001";

    @Pact(consumer = "scli-contract-tests")
    V4Pact iniciarSesion(PactDslWithProvider builder) {
        PactDslJsonBody requestBody = new PactDslJsonBody()
                .stringValue("username", "admin")
                .stringValue("password", "Admin123!");

        PactDslJsonBody responseBody = new PactDslJsonBody();
        responseBody.stringValue("tokenType", "Bearer")
                .stringType("accessToken", "access-token")
                .stringType("refreshToken", "refresh-token")
                .integerType("expiresIn", 900L)
                .object("usuario")
                    .uuid("id", USUARIO_ID)
                    .uuid("perfilId", PERFIL_ID)
                    .stringValue("username", "admin")
                    .stringValue("nombres", "Administrador")
                    .stringValue("apellidos", "del Sistema")
                    .stringMatcher("emailInstitucional", ".+@.+", "admin@scli.local")
                    .array("roles")
                        .stringType("ADMINISTRADOR")
                    .closeArray()
                    .array("permisos")
                        .stringType("USUARIO_LEER")
                    .closeArray()
                    .array("tiposPerfil")
                        .stringType("ADMINISTRADOR")
                    .closeArray()
                .closeObject();

        return builder
                .given("existe un usuario activo con credenciales válidas")
                .uponReceiving("una solicitud de inicio de sesión")
                    .path("/api/v1/auth/login")
                    .method("POST")
                    .headers(Map.of("Content-Type", "application/json"))
                    .body(requestBody)
                .willRespondWith()
                    .status(200)
                    .headers(Map.of("Content-Type", "application/json"))
                    .body(responseBody)
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "iniciarSesion")
    void iniciaSesionConLaEstructuraReal(MockServer mockServer) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServer.getUrl() + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\"admin\",\"password\":\"Admin123!\"}"))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers()
                .firstValue("Content-Type").orElseThrow());
        assertTrue(response.body().contains("\"tokenType\":\"Bearer\""));
        assertTrue(response.body().contains("\"accessToken\""));
        assertTrue(response.body().contains("\"refreshToken\""));
        assertTrue(response.body().contains("\"expiresIn\":900"));
        assertTrue(response.body().contains("\"usuario\""));
        assertTrue(response.body().contains("\"id\":\"" + USUARIO_ID + "\""));
        assertTrue(response.body().contains("\"perfilId\":\"" + PERFIL_ID + "\""));
        assertTrue(response.body().contains("\"roles\""));
        assertTrue(response.body().contains("\"permisos\""));
        assertTrue(response.body().contains("\"tiposPerfil\""));
    }
}
