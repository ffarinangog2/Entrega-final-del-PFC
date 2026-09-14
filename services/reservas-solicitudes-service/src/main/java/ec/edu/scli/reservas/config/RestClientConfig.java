package ec.edu.scli.reservas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Configura los clientes HTTP para la comunicación interna entre microservicios. */
@Configuration
public class RestClientConfig {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    @Bean("usuariosRestClient")
    public RestClient usuariosRestClient(
            RestClient.Builder builder,
            @Value("${app.services.usuarios.base-url}") String baseUrl,
            @Value("${app.internal-api-key}") String internalApiKey,
            @Value("${app.http.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${app.http.read-timeout-ms}") long readTimeoutMs) {
        return createRestClient(builder, baseUrl, internalApiKey, connectTimeoutMs, readTimeoutMs);
    }

    @Bean("academicoRestClient")
    public RestClient academicoRestClient(
            RestClient.Builder builder,
            @Value("${app.services.academico-laboratorios.base-url}") String baseUrl,
            @Value("${app.internal-api-key}") String internalApiKey,
            @Value("${app.http.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${app.http.read-timeout-ms}") long readTimeoutMs) {
        return createRestClient(builder, baseUrl, internalApiKey, connectTimeoutMs, readTimeoutMs);
    }

    private RestClient createRestClient(
            RestClient.Builder builder,
            String baseUrl,
            String internalApiKey,
            long connectTimeoutMs,
            long readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return builder.clone()
                .baseUrl(baseUrl)
                .defaultHeader(INTERNAL_API_KEY_HEADER, internalApiKey)
                .requestFactory(requestFactory)
                .build();
    }
}
