package ec.edu.uteq.scli.api_gateway.contracts;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ec.edu.scli.contracts.RuntimeContractVerifier;
import ec.edu.scli.contracts.RuntimeContractVerifier.Operation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class GatewayOpenApiRuntimeCompletenessTest {
    private static final Map<String, String> CONTRACTS = Map.of(
            "auth", "docs/openapi/auth-service-openapi.json",
            "usuarios", "docs/openapi/usuarios-service-openapi.json",
            "academico", "docs/openapi/academico-laboratorios-service-openapi.json",
            "reservas", "docs/openapi/reservas-solicitudes-service-openapi.json");
    private static final Map<String, BackendProbe> PROBES = new LinkedHashMap<>();

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void startBackends() {
        ensureBackendsStarted();
    }

    @AfterAll
    static void stopBackends() {
        PROBES.values().forEach(BackendProbe::close);
    }

    @DynamicPropertySource
    static void backendUrls(DynamicPropertyRegistry registry) {
        ensureBackendsStarted();
        registry.add("AUTH_SERVICE_URL", () -> PROBES.get("auth").url());
        registry.add("USUARIOS_SERVICE_URL", () -> PROBES.get("usuarios").url());
        registry.add("ACADEMICO_SERVICE_URL", () -> PROBES.get("academico").url());
        registry.add("RESERVAS_SOLICITUDES_SERVICE_URL", () -> PROBES.get("reservas").url());
    }

    @Test
    void fachadaSeDerivaDeBackendsValidadosYRouterFunctionsReales() throws Exception {
        Map<Operation, String> canonical = new HashMap<>();
        Map<String, List<Operation>> backendOperations = new HashMap<>();
        for (Map.Entry<String, String> entry : CONTRACTS.entrySet()) {
            List<Operation> operations = RuntimeContractVerifier.openApiOperations(
                    RuntimeContractVerifier.repositoryFile(entry.getValue()));
            backendOperations.put(entry.getKey(), operations);
            for (Operation operation : operations) {
                if (!isInternal(operation.path())) {
                    String previous = canonical.put(operation, entry.getKey());
                    assertThat(previous)
                            .as("una operacion publica pertenece a un solo backend: %s", operation)
                            .isNull();
                }
            }
        }

        Map<Operation, String> expected = new HashMap<>(canonical);
        addAliases(expected, backendOperations.get("auth"), "auth", "/auth-service");
        addAliases(expected, backendOperations.get("usuarios"), "usuarios", "/usuarios-service");

        List<Operation> gateway = RuntimeContractVerifier.openApiOperations(
                RuntimeContractVerifier.repositoryFile("docs/openapi/api-gateway-openapi.json"));

        assertThat(canonical).hasSize(182);
        assertThat(expected.keySet().stream()
                .filter(operation -> operation.path().startsWith("/auth-service/"))).hasSize(8);
        assertThat(expected.keySet().stream()
                .filter(operation -> operation.path().startsWith("/usuarios-service/"))).hasSize(34);
        assertThat(expected).hasSize(224);
        assertThat(gateway).hasSize(224);
        RuntimeContractVerifier.assertMatches(expected.keySet(), gateway);

        for (Operation operation : gateway) {
            assertRouted(operation, expected.get(operation));
        }
    }

    @Test
    void backendPublicoNoCubiertoYGatewaySinBackendSonDetectados() {
        Operation backendNotRouted = new Operation("GET", "/api/v1/nueva-familia");
        Operation gatewayWithoutBackend = new Operation("GET", "/api/v1/fantasma");

        assertThat(RuntimeContractVerifier.compare(
                List.of(backendNotRouted), List.of(gatewayWithoutBackend)))
                .anyMatch(problem -> problem.startsWith("MISSING_IN_CONTRACT"))
                .anyMatch(problem -> problem.startsWith("EXTRA_IN_CONTRACT"));
    }

    private void assertRouted(Operation operation, String expectedService) throws Exception {
        assertThat(expectedService).as("backend de %s", operation).isNotNull();
        PROBES.values().forEach(BackendProbe::clear);
        String concretePath = concretePath(operation.path());

        mockMvc.perform(MockMvcRequestBuilders.request(
                        HttpMethod.valueOf(operation.method()), concretePath))
                .andExpect(status().isNoContent());

        for (Map.Entry<String, BackendProbe> probe : PROBES.entrySet()) {
            if (probe.getKey().equals(expectedService)) {
                assertThat(probe.getValue().requests())
                        .as("request enviada al backend %s", expectedService)
                        .containsExactly(operation.method() + " " + expectedBackendPath(concretePath));
            } else {
                assertThat(probe.getValue().requests()).isEmpty();
            }
        }
    }

    private static void addAliases(
            Map<Operation, String> expected, List<Operation> backend,
            String service, String prefix) {
        for (Operation operation : backend) {
            if (!isInternal(operation.path())) {
                Operation alias = new Operation(operation.method(), prefix + operation.path());
                assertThat(expected.put(alias, service)).isNull();
            }
        }
    }

    private static boolean isInternal(String path) {
        return path.startsWith("/api/v1/internal/") || path.equals("/api/v1/internal");
    }

    private static String expectedBackendPath(String gatewayPath) {
        for (String prefix : List.of("/auth-service", "/usuarios-service")) {
            if (gatewayPath.startsWith(prefix + "/")) {
                return gatewayPath.substring(prefix.length());
            }
        }
        return gatewayPath;
    }

    private static String concretePath(String template) {
        StringBuilder result = new StringBuilder();
        int variable = 0;
        for (int index = 0; index < template.length(); index++) {
            char character = template.charAt(index);
            if (character == '{') {
                int close = template.indexOf('}', index);
                if (close < 0) {
                    throw new IllegalArgumentException("Path invalido: " + template);
                }
                result.append("00000000-0000-0000-0000-")
                        .append(String.format("%012d", ++variable));
                index = close;
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

    private static synchronized void ensureBackendsStarted() {
        if (!PROBES.isEmpty()) {
            return;
        }
        try {
            for (String service : CONTRACTS.keySet()) {
                PROBES.put(service, new BackendProbe());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudieron iniciar backends de prueba", exception);
        }
    }

    private static final class BackendProbe implements AutoCloseable {
        private final HttpServer server;
        private final ConcurrentLinkedQueue<String> requests = new ConcurrentLinkedQueue<>();

        private BackendProbe() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
            server.start();
        }

        private String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private List<String> requests() {
            return new ArrayList<>(requests);
        }

        private void clear() {
            requests.clear();
        }

        private void handle(HttpExchange exchange) throws IOException {
            exchange.getRequestBody().readAllBytes();
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
