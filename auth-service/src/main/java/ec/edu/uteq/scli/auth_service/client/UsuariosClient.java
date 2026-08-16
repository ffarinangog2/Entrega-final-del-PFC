package ec.edu.uteq.scli.auth_service.client;

import ec.edu.uteq.scli.auth_service.dto.PerfilAuthResponse;
import ec.edu.uteq.scli.auth_service.exception.UsuarioServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

@Service
public class UsuariosClient {

    private final RestClient restClient;
    private final String internalApiKey;

    public UsuariosClient(
            @Value("${usuarios.service.url:http://localhost:8082}") String baseUrl,
            @Value("${app.internal-api-key}") String internalApiKey,
            @Value("${usuarios.service.timeout-ms:3000}") long timeoutMs) {

        this.internalApiKey = internalApiKey;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public PerfilAuthResponse obtenerPerfil(UUID perfilId) {
        try {
            return restClient.get()
                    .uri("/api/v1/internal/perfiles/{id}", perfilId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .body(PerfilAuthResponse.class);

        } catch (RestClientException excepcion) {
            throw new UsuarioServiceUnavailableException(
                    "No se pudo contactar a usuarios-service para el perfil " + perfilId,
                    excepcion);
        }
    }
}