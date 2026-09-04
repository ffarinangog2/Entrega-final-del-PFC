package ec.edu.scli.reservas.client;

import ec.edu.scli.reservas.client.dto.PerfilExternoResponse;
import ec.edu.scli.reservas.client.dto.ContextoInstitucionalExternoResponse;
import ec.edu.scli.reservas.client.dto.DocenteExternoResponse;
import ec.edu.scli.reservas.client.dto.EstudianteExternoResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.UUID;
import java.util.List;
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

    public ContextoInstitucionalExternoResponse obtenerContextoInstitucional(UUID perfilId) {
        return executeWithReadRetries(() -> restClient.get()
                .uri("/api/v1/internal/perfiles/{perfilId}/contexto-institucional", perfilId)
                .retrieve().body(ContextoInstitucionalExternoResponse.class));
    }

    public DocenteExternoResponse obtenerDocentePorPerfil(UUID perfilId) {
        return executeWithReadRetries(() -> restClient.get()
                .uri("/api/v1/internal/docentes/perfil/{perfilId}", perfilId)
                .retrieve().body(DocenteExternoResponse.class));
    }

    public DocenteExternoResponse obtenerDocentePorId(UUID docenteId) {
        return executeWithReadRetries(() -> restClient.get()
                .uri("/api/v1/internal/docentes/{docenteId}", docenteId)
                .retrieve().body(DocenteExternoResponse.class));
    }
    public boolean docentePerteneceCarrera(UUID docenteId, UUID carreraId) {
        Boolean response = executeWithReadRetries(() -> restClient.get()
                .uri("/api/v1/internal/docentes/{docenteId}/carreras/{carreraId}/exists",
                        docenteId, carreraId)
                .retrieve().body(Boolean.class));
        return Boolean.TRUE.equals(response);
    }

    public EstudianteExternoResponse obtenerEstudiantePorPerfil(UUID perfilId) {
        return executeWithReadRetries(() -> restClient.get()
                .uri("/api/v1/internal/estudiantes/perfil/{perfilId}", perfilId)
                .retrieve().body(EstudianteExternoResponse.class));
    }
    public EstudianteExternoResponse obtenerContextoEstudiante(UUID perfilId, UUID periodoId) {
        return executeWithReadRetries(() -> restClient.get()
                .uri("/api/v1/internal/estudiantes/perfil/{perfilId}/periodo/{periodoId}",perfilId,periodoId)
                .retrieve().body(EstudianteExternoResponse.class));
    }

    public List<UUID> obtenerAdministradoresPorPiso(UUID pisoId) {
        UUID[] perfiles = executeWithReadRetries(() -> restClient.get()
                .uri(uri -> uri.path("/api/v1/internal/administradores/por-piso")
                        .queryParam("pisoId", pisoId).build())
                .retrieve().body(UUID[].class));
        return perfiles == null ? List.of() : List.of(perfiles);
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
