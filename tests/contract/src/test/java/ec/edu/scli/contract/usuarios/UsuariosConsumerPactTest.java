package ec.edu.scli.contract.usuarios;

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
@PactTestFor(providerName = "usuarios-service")
class UsuariosConsumerPactTest {

    private static final String PERFIL_ID = "55555555-5555-5555-5555-555555555555";

    @Pact(consumer = "scli-contract-tests")
    V4Pact obtenerPerfilPorId(PactDslWithProvider builder) {
        PactDslJsonBody body = perfilBody();

        return builder
                .given("existe un perfil con id " + PERFIL_ID)
                .uponReceiving("una consulta de un perfil por id")
                    .path("/api/v1/perfiles/" + PERFIL_ID)
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .headers(Map.of("Content-Type", "application/json"))
                    .body(body)
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "scli-contract-tests")
    V4Pact listarPerfiles(PactDslWithProvider builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.minArrayLike("content", 1)
                    .uuid("id", PERFIL_ID)
                    .stringValue("identificacion", "0102030405")
                    .stringValue("nombres", "Ana")
                    .stringValue("apellidos", "Gomez")
                    .stringMatcher("emailInstitucional", ".+@.+", "ana.gomez@uteq.edu.ec")
                    .stringMatcher("emailPersonal", ".+@.+", "ana.gomez@gmail.com")
                    .stringValue("telefono", "0999999999")
                    .stringValue("direccion", "Av. Principal 123")
                    .stringMatcher("fechaNacimiento", "\\d{4}-\\d{2}-\\d{2}", "1995-05-20")
                    .stringValue("fotoUrl", "https://cdn.scli.edu.ec/perfiles/ana.png")
                    .booleanValue("activo", true)
                    .stringMatcher("creadoEn", ".+Z", "2026-08-18T10:00:00Z")
                    .stringMatcher("actualizadoEn", ".+Z", "2026-08-18T10:00:00Z")
                .closeObject()
                .closeArray();
        body.integerType("totalElements", 1L)
                .integerType("totalPages", 1);

        return builder
                .given("existen perfiles registrados")
                .uponReceiving("una consulta del listado de perfiles")
                    .path("/api/v1/perfiles")
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .headers(Map.of("Content-Type", "application/json"))
                    .body(body)
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "obtenerPerfilPorId")
    void obtienePerfilPorIdConLaEstructuraReal(MockServer mockServer) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServer.getUrl() + "/api/v1/perfiles/" + PERFIL_ID))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers()
                .firstValue("Content-Type").orElseThrow());
        assertTrue(response.body().contains("\"id\":\"" + PERFIL_ID + "\""));
        assertTrue(response.body().contains("\"identificacion\":\"0102030405\""));
        assertTrue(response.body().contains("\"activo\":true"));
    }

    @Test
    @PactTestFor(pactMethod = "listarPerfiles")
    void listaPerfilesConLaEstructuraPaginadaReal(MockServer mockServer) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServer.getUrl() + "/api/v1/perfiles"))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers()
                .firstValue("Content-Type").orElseThrow());
        assertTrue(response.body().contains("\"content\""));
        assertTrue(response.body().contains("\"id\":\"" + PERFIL_ID + "\""));
        assertTrue(response.body().contains("\"totalElements\":1"));
    }

    private PactDslJsonBody perfilBody() {
        return new PactDslJsonBody()
                .uuid("id", PERFIL_ID)
                .stringValue("identificacion", "0102030405")
                .stringValue("nombres", "Ana")
                .stringValue("apellidos", "Gomez")
                .stringMatcher("emailInstitucional", ".+@.+", "ana.gomez@uteq.edu.ec")
                .stringMatcher("emailPersonal", ".+@.+", "ana.gomez@gmail.com")
                .stringValue("telefono", "0999999999")
                .stringValue("direccion", "Av. Principal 123")
                .stringMatcher("fechaNacimiento", "\\d{4}-\\d{2}-\\d{2}", "1995-05-20")
                .stringValue("fotoUrl", "https://cdn.scli.edu.ec/perfiles/ana.png")
                .booleanValue("activo", true)
                .stringMatcher("creadoEn", ".+Z", "2026-08-18T10:00:00Z")
                .stringMatcher("actualizadoEn", ".+Z", "2026-08-18T10:00:00Z");
    }
}
