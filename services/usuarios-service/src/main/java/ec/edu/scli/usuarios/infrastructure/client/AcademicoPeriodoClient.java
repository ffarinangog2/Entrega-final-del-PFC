package ec.edu.scli.usuarios.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

@Service
public class AcademicoPeriodoClient {
    private final RestClient client;
    private final String key;

    public AcademicoPeriodoClient(
            @Value("${academico.service.url:http://academico-laboratorios-service:8083}") String url,
            @Value("${app.internal-api-key}") String key,
            @Value("${academico.service.timeout-ms:3000}") long timeout) {
        this.key = key;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeout)).build());
        factory.setReadTimeout(Duration.ofMillis(timeout));
        this.client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    public UUID periodoVigente() {
        var value = client.get()
                .uri("/api/v1/internal/periodos-lectivos/actual/contexto")
                .header("X-Internal-Api-Key", key)
                .retrieve()
                .body(PeriodoActual.class);
        if (value == null) {
            throw new IllegalStateException("No existe período académico vigente");
        }
        return value.id();
    }

    public CarreraEstadoResponse estadoCarrera(UUID carreraId) {
        try {
            var value = client.get()
                    .uri("/api/v1/internal/carreras/{id}/estado", carreraId)
                    .header("X-Internal-Api-Key", key)
                    .retrieve()
                    .body(CarreraEstadoResponse.class);
            if (value == null) {
                return new CarreraEstadoResponse(carreraId, false, false);
            }
            return value;
        } catch (Exception e) {
            return new CarreraEstadoResponse(carreraId, false, false);
        }
    }

    private record PeriodoActual(UUID id) {}
    public record CarreraEstadoResponse(UUID id, boolean existe, boolean activa) {}
}
