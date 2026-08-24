package ec.edu.scli.reservas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

/** Documenta el esquema JWT que ya protege las operaciones de escritura. */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI reservasOpenApi() {
        return new OpenAPI()
                .info(new Info().title("SCLI - Reservas y Solicitudes API")
                        .version("1.0.0")
                        .description("Gestión segura e idempotente de solicitudes, reservas y agenda"))
                .servers(List.of(new Server().url("/").description("Servidor actual")))
                .components(new Components().addSecuritySchemes(
                        BEARER_AUTH,
                        new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }

    @Bean
    OperationCustomizer documentarJwtEnEscrituras() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.getBeanType().getPackageName()
                    .startsWith("ec.edu.scli.reservas.presentation.controller")) {
                operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
            }
            operation.getResponses().addApiResponse("400", new ApiResponse().description("Petición inválida"));
            operation.getResponses().addApiResponse("401", new ApiResponse().description("Access token ausente o inválido"));
            operation.getResponses().addApiResponse("403", new ApiResponse().description("Permiso u ownership insuficiente"));
            operation.getResponses().addApiResponse("404", new ApiResponse().description("Recurso inexistente"));
            operation.getResponses().addApiResponse("409", new ApiResponse().description("Conflicto de estado, concurrencia o idempotencia"));
            documentarEstadoHttpReal(operation, handlerMethod.getBeanType(),
                    handlerMethod.getMethod().getName());
            return operation;
        };
    }

    private void documentarEstadoHttpReal(
            io.swagger.v3.oas.models.Operation operation, Class<?> controller, String metodo) {
        String controllerName = controller.getSimpleName();
        if ((controllerName.equals("SolicitudReservaController") && metodo.equals("crear"))
                || (controllerName.equals("AgendaController") && metodo.equals("crearBloqueo"))) {
            moverRespuesta(operation, "200", "201", "Creado");
        } else if (controllerName.equals("AgendaController") && metodo.equals("eliminarBloqueo")) {
            moverRespuesta(operation, "200", "204", "Eliminado sin contenido");
        }
    }

    private void moverRespuesta(
            io.swagger.v3.oas.models.Operation operation,
            String origen,
            String destino,
            String descripcion) {
        ApiResponse response = operation.getResponses().remove(origen);
        if (response == null) {
            response = new ApiResponse();
        }
        response.setDescription(descripcion);
        operation.getResponses().addApiResponse(destino, response);
    }
}
