package ec.edu.scli.contract.reservas;

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
@PactTestFor(providerName = "reservas-solicitudes-service")
class ReservasConsumerPactTest {

    private static final String RESERVA_ID = "11111111-1111-1111-1111-111111111111";
    private static final String SOLICITUD_ID = "22222222-2222-2222-2222-222222222222";
    private static final String LABORATORIO_ID = "33333333-3333-3333-3333-333333333333";
    private static final String RESPONSABLE_ID = "44444444-4444-4444-4444-444444444444";

    @Pact(consumer = "scli-contract-tests")
    V4Pact listarReservas(PactDslWithProvider builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.minArrayLike("contenido", 1)
                    .uuid("id", RESERVA_ID)
                    .uuid("solicitudId", SOLICITUD_ID)
                    .uuid("laboratorioId", LABORATORIO_ID)
                    .uuid("responsableId", RESPONSABLE_ID)
                    .stringMatcher("fechaReserva", "\\d{4}-\\d{2}-\\d{2}", "2026-08-20")
                    .stringMatcher("horaInicio", "\\d{2}:\\d{2}:\\d{2}", "08:00:00")
                    .stringMatcher("horaFin", "\\d{2}:\\d{2}:\\d{2}", "10:00:00")
                    .stringValue("estado", "PROGRAMADA")
                    .stringValue("codigoReserva", "RES-2026-0001")
                    .stringMatcher("creadaEn", ".+Z", "2026-08-18T10:00:00Z")
                    .stringMatcher("actualizadaEn", ".+Z", "2026-08-18T10:00:00Z")
                    .integerType("version", 0L)
                .closeObject()
                .closeArray();
        body.integerType("pagina", 0)
                .integerType("tamanio", 20)
                .integerType("totalElementos", 1L)
                .integerType("totalPaginas", 1)
                .booleanValue("primera", true)
                .booleanValue("ultima", true);

        return builder
                .given("existen reservas registradas")
                .uponReceiving("una consulta del listado de reservas")
                    .path("/api/v1/reservas")
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .headers(Map.of("Content-Type", "application/json"))
                    .body(body)
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "scli-contract-tests")
    V4Pact buscarReservaPorId(PactDslWithProvider builder) {
        PactDslJsonBody body = new PactDslJsonBody()
                .uuid("id", RESERVA_ID)
                .uuid("solicitudId", SOLICITUD_ID)
                .uuid("laboratorioId", LABORATORIO_ID)
                .uuid("responsableId", RESPONSABLE_ID)
                .stringMatcher("fechaReserva", "\\d{4}-\\d{2}-\\d{2}", "2026-08-20")
                .stringMatcher("horaInicio", "\\d{2}:\\d{2}:\\d{2}", "08:00:00")
                .stringMatcher("horaFin", "\\d{2}:\\d{2}:\\d{2}", "10:00:00")
                .stringValue("estado", "PROGRAMADA")
                .stringValue("codigoReserva", "RES-2026-0001")
                .stringMatcher("creadaEn", ".+Z", "2026-08-18T10:00:00Z")
                .stringMatcher("actualizadaEn", ".+Z", "2026-08-18T10:00:00Z")
                .integerType("version", 0L);

        return builder
                .given("existe una reserva con id " + RESERVA_ID)
                .uponReceiving("una consulta de una reserva por id")
                    .path("/api/v1/reservas/" + RESERVA_ID)
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .headers(Map.of("Content-Type", "application/json"))
                    .body(body)
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "listarReservas")
    void listaReservasConLaEstructuraPaginadaReal(MockServer mockServer) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServer.getUrl() + "/api/v1/reservas"))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers()
                .firstValue("Content-Type").orElseThrow());
        assertTrue(response.body().contains("\"contenido\""));
        assertTrue(response.body().contains("\"id\":\"" + RESERVA_ID + "\""));
        assertTrue(response.body().contains("\"pagina\":0"));
        assertTrue(response.body().contains("\"totalElementos\":1"));
    }

    @Test
    @PactTestFor(pactMethod = "buscarReservaPorId")
    void obtieneReservaPorIdConLaEstructuraReal(MockServer mockServer) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServer.getUrl() + "/api/v1/reservas/" + RESERVA_ID))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers()
                .firstValue("Content-Type").orElseThrow());
        assertTrue(response.body().contains("\"id\":\"" + RESERVA_ID + "\""));
        assertTrue(response.body().contains("\"solicitudId\":\"" + SOLICITUD_ID + "\""));
        assertTrue(response.body().contains("\"laboratorioId\":\"" + LABORATORIO_ID + "\""));
        assertTrue(response.body().contains("\"responsableId\":\"" + RESPONSABLE_ID + "\""));
        assertTrue(response.body().contains("\"estado\":\"PROGRAMADA\""));
        assertTrue(response.body().contains("\"version\":0"));
    }
}
