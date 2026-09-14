package ec.edu.scli.contract.academico;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.dsl.DslPart;
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
@PactTestFor(providerName = "academico-laboratorios-service")
class MobileAcademicoConsumerPactTest {
    private static final String LAB_ID = "33333333-3333-3333-3333-333333333333";

    @Pact(consumer = "scli-mobile")
    V4Pact laboratoriosPaginados(PactDslWithProvider builder) {
        return builder.given("existen laboratorios activos")
                .uponReceiving("Mobile lista laboratorios paginados")
                .path("/api/v1/laboratorios").query("page=0&size=100").method("GET")
                .willRespondWith().status(200).headers(jsonHeaders())
                .body(pagina())
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "scli-mobile")
    V4Pact materiasPaginadas(PactDslWithProvider builder) {
        return builder.given("existen materias activas")
                .uponReceiving("Mobile lista materias paginadas")
                .path("/api/v1/materias").query("page=0&size=100").method("GET")
                .willRespondWith().status(200).headers(jsonHeaders())
                .body(pagina())
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "scli-mobile")
    V4Pact periodoActual(PactDslWithProvider builder) {
        return builder.given("existe un periodo lectivo activo")
                .uponReceiving("Mobile consulta el periodo actual")
                .path("/api/v1/periodos-lectivos/actual").method("GET")
                .willRespondWith().status(200).headers(jsonHeaders())
                .body(new PactDslJsonBody().uuid("id").stringValue("codigo", "2026-A")
                        .stringValue("nombre", "Periodo 2026 A").date("fechaInicio", "yyyy-MM-dd")
                        .date("fechaFin", "yyyy-MM-dd").stringValue("estado", "ACTIVO"))
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "scli-mobile")
    V4Pact detalleCompletoQr(PactDslWithProvider builder) {
        DslPart body = new PactDslJsonBody()
                .object("laboratorio").uuid("id", LAB_ID).uuid("pisoId")
                    .stringValue("codigo", "LAB-01").stringValue("nombre", "Redes")
                    .integerType("capacidad", 30).stringValue("estado", "DISPONIBLE")
                    .booleanValue("activo", true).closeObject()
                .object("piso").uuid("id").uuid("bloqueId").integerType("numero", 2)
                    .stringValue("descripcion", "Planta alta").booleanValue("activo", true).closeObject()
                .object("bloque").uuid("id").uuid("campusId").stringValue("codigo", "B1")
                    .stringValue("nombre", "Bloque 1").booleanValue("activo", true).closeObject()
                .object("campus").uuid("id").stringValue("codigo", "C1")
                    .stringValue("nombre", "Central").booleanValue("activo", true).closeObject()
                .minArrayLike("equipos", 0).closeArray();
        return builder.given("existe el laboratorio para el QR")
                .uponReceiving("Mobile consulta detalle completo desde QR")
                .path("/api/v1/laboratorios/" + LAB_ID + "/detalle-completo").method("GET")
                .willRespondWith().status(200).headers(jsonHeaders()).body(body)
                .toPact(V4Pact.class);
    }

    @Test @PactTestFor(pactMethod = "laboratoriosPaginados")
    void verificaPaginaLaboratorios(MockServer server) throws Exception {
        var response = get(server, "/api/v1/laboratorios?page=0&size=100");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"content\""));
        assertTrue(response.body().contains("\"totalPages\""));
    }

    @Test @PactTestFor(pactMethod = "materiasPaginadas")
    void verificaPaginaMaterias(MockServer server) throws Exception {
        assertTrue(get(server, "/api/v1/materias?page=0&size=100").body().contains("\"content\""));
    }

    @Test @PactTestFor(pactMethod = "periodoActual")
    void verificaEstadoPeriodo(MockServer server) throws Exception {
        assertTrue(get(server, "/api/v1/periodos-lectivos/actual").body().contains("\"estado\":\"ACTIVO\""));
    }

    @Test @PactTestFor(pactMethod = "detalleCompletoQr")
    void verificaDetalleQr(MockServer server) throws Exception {
        assertTrue(get(server, "/api/v1/laboratorios/" + LAB_ID + "/detalle-completo")
                .body().contains("\"numero\":2"));
    }

    private HttpResponse<String> get(MockServer server, String path) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder()
                .uri(URI.create(server.getUrl() + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private Map<String, String> jsonHeaders() {
        return Map.of("Content-Type", "application/json");
    }

    private DslPart pagina() {
        PactDslJsonBody body = new PactDslJsonBody();
        body.minArrayLike("content", 1).uuid("id").closeObject().closeArray();
        body.integerType("number", 0).integerType("size", 100)
                .integerType("totalElements", 1L).integerType("totalPages", 1)
                .integerType("numberOfElements", 1).booleanValue("first", true)
                .booleanValue("last", true).booleanValue("empty", false);
        return body;
    }
}
