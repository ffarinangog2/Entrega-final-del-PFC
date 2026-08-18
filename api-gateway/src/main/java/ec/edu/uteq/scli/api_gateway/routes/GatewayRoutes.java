package ec.edu.uteq.scli.api_gateway.routes;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.stripPrefix;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

@Configuration
public class GatewayRoutes {

    @Bean
    public RouterFunction<ServerResponse> authServiceRoute() {
        return route("auth_service")
                .route(request -> request.path().startsWith("/auth-service/"), http())
                .before(uri("http://localhost:8081"))
                .before(stripPrefix(1))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> usuariosServiceRoute() {
        return route("usuarios_service")
                .route(request -> request.path().startsWith("/usuarios-service/"), http())
                .before(uri("http://localhost:8082"))
                .before(stripPrefix(1))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> reservasSolicitudesServiceRoute(
            @Value("${RESERVAS_SOLICITUDES_SERVICE_URL:http://reservas-solicitudes-service:8084}")
            String reservasSolicitudesServiceUrl) {
        return route("reservas_solicitudes_service")
                .route(request -> esRutaReservas(request.path()), http())
                .before(uri(reservasSolicitudesServiceUrl))
                .build();
    }

    private boolean esRutaReservas(String path) {
        return path.equals("/api/v1/reservas") || path.startsWith("/api/v1/reservas/")
                || path.equals("/api/v1/solicitudes") || path.startsWith("/api/v1/solicitudes/")
                || path.equals("/api/v1/agenda") || path.startsWith("/api/v1/agenda/")
                || path.equals("/api/v1/disponibilidad") || path.startsWith("/api/v1/disponibilidad/");
    }
}
