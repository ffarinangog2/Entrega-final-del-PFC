package ec.edu.scli.academico.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class UsuariosContextoClient {
    private final RestClient client;
    private final String internalApiKey;

    public UsuariosContextoClient(RestClient.Builder builder,
            @Value("${usuarios.service.url:http://localhost:8082}") String baseUrl,
            @Value("${app.internal-api-key}") String internalApiKey) {
        this.client = builder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    public ContextoInstitucionalResponse obtener(UUID perfilId) {
        return client.get()
                .uri("/api/v1/internal/perfiles/{perfilId}/contexto-institucional", perfilId)
                .header("X-Internal-Api-Key", internalApiKey)
                .retrieve()
                .body(ContextoInstitucionalResponse.class);
    }
}
