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

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "reservas-solicitudes-service")
class PlanificacionAsistenciaConsumerPactTest {
    private static final String PLAN_ID = "11111111-2222-3333-4444-555555555555";
    private static final String SESION_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @Pact(consumer = "scli-web")
    V4Pact listarPlanificacion(PactDslWithProvider builder) {
        return builder.given("existe planificacion en el ambito autenticado")
                .uponReceiving("Web consulta planificacion semestral")
                .path("/api/v1/planificaciones").method("GET")
                .willRespondWith().status(200).headers(Map.of("Content-Type", "application/json"))
                .body("[{\"id\":\"" + PLAN_ID + "\",\"diaSemana\":\"LUNES\",\"horaInicio\":\"08:00:00\",\"horaFin\":\"10:00:00\",\"estado\":\"ENVIADA\"}]")
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "scli-mobile")
    V4Pact historialAsistencia(PactDslWithProvider builder) {
        return builder.given("estudiante autenticado tiene asistencia")
                .uponReceiving("Mobile consulta historial propio de asistencia")
                .path("/api/v1/asistencias/historial").method("GET")
                .willRespondWith().status(200).headers(Map.of("Content-Type", "application/json"))
                .body("[{\"id\":\"11111111-1111-1111-1111-111111111111\",\"sesionId\":\"" + SESION_ID
                        + "\",\"estudianteId\":\"44444444-4444-4444-4444-444444444444\",\"registradaEn\":\"2026-09-01T14:00:00Z\",\"estado\":\"PRESENTE\"}]")
                .toPact(V4Pact.class);
    }

    @Test @PactTestFor(pactMethod = "listarPlanificacion")
    void webLista(MockServer server) throws Exception { assertEquals(200, get(server, "/api/v1/planificaciones")); }

    @Test @PactTestFor(pactMethod = "historialAsistencia")
    void mobileHistorial(MockServer server) throws Exception { assertEquals(200, get(server, "/api/v1/asistencias/historial")); }

    private int get(MockServer server, String path) throws Exception {
        var request = HttpRequest.newBuilder().uri(URI.create(server.getUrl() + path)).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
    }
}
