package ec.edu.scli.reservas.client;

import ec.edu.scli.reservas.client.dto.ExisteExternoResponse;
import ec.edu.scli.reservas.client.dto.LaboratorioExternoResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.UUID;
import java.util.function.Supplier;

/** Cliente REST para consultar datos académicos y laboratorios. */
@Component
public class AcademicoLaboratoriosClient {

    private final RestClient restClient;
    private final int maxReadRetries;

    public AcademicoLaboratoriosClient(
            @Qualifier("academicoRestClient") RestClient restClient,
            @Value("${app.http.max-read-retries:2}") int maxReadRetries) {
        this.restClient = restClient;
        this.maxReadRetries = Math.max(0, maxReadRetries);
    }

    public LaboratorioExternoResponse obtenerLaboratorio(UUID laboratorioId) {
        return executeWithReadRetries(() -> restClient.get()
                .uri("/api/v1/internal/laboratorios/{id}/disponibilidad-base", laboratorioId)
                .retrieve()
                .body(LaboratorioExternoResponse.class));
    }

    public ExisteExternoResponse verificarMateria(UUID materiaId) {
        return executeWithReadRetries(() -> restClient.get()
                .uri("/api/v1/internal/materias/{id}/exists", materiaId)
                .retrieve()
                .body(ExisteExternoResponse.class));
    }

    public ExisteExternoResponse verificarPeriodoLectivo(UUID periodoLectivoId) {
        return executeWithReadRetries(() -> restClient.get()
                .uri("/api/v1/internal/periodos-lectivos/{id}/exists", periodoLectivoId)
                .retrieve()
                .body(ExisteExternoResponse.class));
    }

    public boolean existeMateria(UUID materiaId) {
        ExisteExternoResponse response = verificarMateria(materiaId);
        return response != null && response.existe();
    }

    public boolean existePeriodoLectivo(UUID periodoLectivoId) {
        ExisteExternoResponse response = verificarPeriodoLectivo(periodoLectivoId);
        return response != null && response.existe();
    }

    private <T> T executeWithReadRetries(Supplier<T> operation) {
        int retryCount = 0;
        while (true) {
            try {
                return operation.get();
            } catch (ResourceAccessException | HttpServerErrorException exception) {
                if (retryCount >= maxReadRetries) {
                    throw exception;
                }
                retryCount++;
            }
        }
    }
}
