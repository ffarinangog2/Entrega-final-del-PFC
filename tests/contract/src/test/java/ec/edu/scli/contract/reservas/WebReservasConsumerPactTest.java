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
class WebReservasConsumerPactTest {

    private static final String RESERVA_ID = "11111111-1111-1111-1111-111111111111";
    private static final String SOLICITUD_ID = "22222222-2222-2222-2222-222222222222";
    private static final String LABORATORIO_ID = "33333333-3333-3333-3333-333333333333";
    private static final String RESPONSABLE_ID = "44444444-4444-4444-4444-444444444444";

    @Pact(consumer = "scli-web")
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
                .uponReceiving("Web consulta el listado de reservas")
                    .path("/api/v1/reservas")
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .headers(Map.of("Content-Type", "application/json"))
                    .body(body)
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "scli-web")
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
                .uponReceiving("Web consulta una reserva por id")
                    .path("/api/v1/reservas/" + RESERVA_ID)
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .headers(Map.of("Content-Type", "application/json"))
                    .body(body)
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "scli-web")
    V4Pact cancelarReservaProtegida(PactDslWithProvider builder) {
        PactDslJsonBody body = new PactDslJsonBody()
                .uuid("id", RESERVA_ID).uuid("solicitudId", SOLICITUD_ID)
                .uuid("laboratorioId", LABORATORIO_ID).uuid("responsableId", RESPONSABLE_ID)
                .stringValue("fechaReserva", "2026-08-20")
                .stringValue("horaInicio", "08:00:00").stringValue("horaFin", "10:00:00")
                .stringValue("estado", "CANCELADA").stringValue("codigoReserva", "RES-2026-0001")
                .stringValue("creadaEn", "2026-08-18T10:00:00Z")
                .stringValue("actualizadaEn", "2026-08-18T10:00:00Z").integerType("version", 1L);
        return builder.given("existe una reserva programada cancelable")
                .uponReceiving("Web cancela una reserva protegida")
                .path("/api/v1/reservas/" + RESERVA_ID + "/cancelar").method("POST")
                .headers(Map.of("Authorization", "Bearer pact-access-token",
                        "Content-Type", "application/json"))
                .body(new PactDslJsonBody().stringValue("motivo", "Mantenimiento"))
                .willRespondWith().status(200)
                .headers(Map.of("Content-Type", "application/json")).body(body)
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "scli-web")
    V4Pact proponerAlternativaProtegida(PactDslWithProvider builder) {
        PactDslJsonBody respuesta = new PactDslJsonBody()
                .uuid("id", SOLICITUD_ID)
                .stringValue("estado", "PROPUESTA")
                .stringValue("propuestaFecha", "2026-08-21")
                .stringValue("propuestaHoraInicio", "12:00:00")
                .stringValue("propuestaHoraFin", "14:00:00")
                .uuid("propuestaLaboratorioId", LABORATORIO_ID)
                .stringValue("propuestaObservacion", "Horario alternativo");
        PactDslJsonBody solicitud = new PactDslJsonBody()
                .stringValue("fecha", "2026-08-21")
                .stringValue("horaInicio", "12:00:00")
                .stringValue("horaFin", "14:00:00")
                .uuid("laboratorioId", LABORATORIO_ID)
                .stringValue("observacion", "Horario alternativo");
        return builder.given("existe una solicitud en revision del piso administrado")
                .uponReceiving("Web propone un horario alternativo")
                .path("/api/v1/solicitudes/" + SOLICITUD_ID + "/propuesta").method("POST")
                .headers(Map.of("Authorization", "Bearer pact-access-token",
                        "Content-Type", "application/json"))
                .body(solicitud)
                .willRespondWith().status(200)
                .headers(Map.of("Content-Type", "application/json")).body(respuesta)
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "listarReservas")
    void webListaReservasConEstructuraPaginada(MockServer mockServer) throws Exception {
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
    }

    @Test
    @PactTestFor(pactMethod = "buscarReservaPorId")
    void webObtieneReservaPorId(MockServer mockServer) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServer.getUrl() + "/api/v1/reservas/" + RESERVA_ID))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"id\":\"" + RESERVA_ID + "\""));
    }

    @Test
    @PactTestFor(pactMethod = "cancelarReservaProtegida")
    void webCancelaReservaConBearer(MockServer mockServer) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServer.getUrl() + "/api/v1/reservas/" + RESERVA_ID + "/cancelar"))
                .header("Authorization", "Bearer pact-access-token")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"motivo\":\"Mantenimiento\"}"))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"estado\":\"CANCELADA\""));
    }

    @Test
    @PactTestFor(pactMethod = "proponerAlternativaProtegida")
    void webProponeAlternativa(MockServer mockServer) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServer.getUrl() + "/api/v1/solicitudes/" + SOLICITUD_ID + "/propuesta"))
                .header("Authorization", "Bearer pact-access-token")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"fecha":"2026-08-21","horaInicio":"12:00:00","horaFin":"14:00:00",
                         "laboratorioId":"33333333-3333-3333-3333-333333333333",
                         "observacion":"Horario alternativo"}
                        """))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"estado\":\"PROPUESTA\""));
    }
}
