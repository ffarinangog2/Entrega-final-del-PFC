package ec.edu.scli.reservas.client;

import ec.edu.scli.reservas.client.dto.PerfilExternoResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.UUID;
import java.util.function.Supplier;

/** Cliente REST para consultar perfiles en el microservicio de usuarios. */
@Component
public class UsuariosClient {

    private final RestClient restClient;
    private final int maxReadRetries;

    public UsuariosClient(
            @Qualifier("usuariosRestClient") RestClient restClient,
            @Value("${app.http.max-read-retries:2}") int maxReadRetries) {
        this.restClient = restClient;
        this.maxReadRetries = Math.max(0, maxReadRetries);
    }

    public PerfilExternoResponse obtenerPerfil(UUID perfilId) {
        return executeWithReadRetries(() -> restClient.get()
                .uri("/api/v1/internal/perfiles/{perfilId}/exists", perfilId)
                .retrieve()
                .body(PerfilExternoResponse.class));
    }

    public boolean existePerfilActivo(UUID perfilId) {
        PerfilExternoResponse response = obtenerPerfil(perfilId);
        return response != null && response.existe() && response.activo();
    }

    public boolean existeDocenteActivo(UUID perfilId) {
        PerfilExternoResponse response = obtenerPerfil(perfilId);
        return response != null
                && response.existe()
                && response.activo()
                && response.tiposPerfil() != null
                && response.tiposPerfil().stream().anyMatch("DOCENTE"::equalsIgnoreCase);
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
