package ec.edu.scli.usuarios.infrastructure.client;

import ec.edu.scli.usuarios.presentation.dto.usuarios.AuthUsuarioCreateRequest;
import ec.edu.scli.usuarios.presentation.dto.usuarios.AuthUsuarioUpdateRequest;
import ec.edu.scli.usuarios.presentation.dto.usuarios.AuthUsuarioResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

@Service
public class AuthAdminClient {
    private final RestClient client;
    private final String internalApiKey;

    public AuthAdminClient(@Value("${auth.service.url:http://auth-service:8081}") String baseUrl,
            @Value("${app.internal-api-key}") String internalApiKey,
            @Value("${auth.service.timeout-ms:3000}") long timeoutMs) {
        this.internalApiKey = internalApiKey;
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public AuthUsuarioResponse crear(AuthUsuarioCreateRequest request) {
        return client.post().uri("/api/v1/internal/admin/usuarios")
                .header("X-Internal-Api-Key", internalApiKey).body(request).retrieve().body(AuthUsuarioResponse.class);
    }

    public AuthUsuarioResponse obtener(UUID authId) {
        return client.get().uri("/api/v1/internal/admin/usuarios/{id}", authId)
                .header("X-Internal-Api-Key", internalApiKey).retrieve().body(AuthUsuarioResponse.class);
    }

    public AuthUsuarioResponse actualizar(UUID authId, AuthUsuarioUpdateRequest request) {
        return client.put().uri("/api/v1/internal/admin/usuarios/{id}", authId)
                .header("X-Internal-Api-Key", internalApiKey).body(request).retrieve().body(AuthUsuarioResponse.class);
    }

    public void eliminarCredencialCreada(UUID authId, UUID perfilId) {
        client.delete().uri(builder -> builder.path("/api/v1/internal/admin/usuarios/{id}")
                        .queryParam("perfilId", perfilId).build(authId))
                .header("X-Internal-Api-Key", internalApiKey).retrieve().toBodilessEntity();
    }
}
