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
        public RouterFunction<ServerResponse> authApiRoute(
                        @Value("${AUTH_SERVICE_URL:http://auth-service:8081}") String authServiceUrl) {
                return route("auth_api")
                                .route(request -> request.path().startsWith("/api/v1/auth/"), http())
                                .before(uri(authServiceUrl))
                                .build();
        }

    @Bean
    public RouterFunction<ServerResponse> authServiceRoute(
            @Value("${AUTH_SERVICE_URL:http://auth-service:8081}") String authServiceUrl) {
        return route("auth_service")
                .route(request -> request.path().startsWith("/auth-service/"), http())
                .before(uri(authServiceUrl))
                .before(stripPrefix(1))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> usuariosServiceRoute(
            @Value("${USUARIOS_SERVICE_URL:http://usuarios-service:8082}") String usuariosServiceUrl) {
        return route("usuarios_service")
                .route(request -> request.path().startsWith("/usuarios-service/"), http())
                .before(uri(usuariosServiceUrl))
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

        @Bean
        public RouterFunction<ServerResponse> academicoServiceRoute(
                        @Value("${ACADEMICO_SERVICE_URL:http://academico-laboratorios-service:8083}")
                        String academicoServiceUrl) {
                return route("academico_service")
                                .route(request -> esRutaAcademica(request.path()), http())
                                .before(uri(academicoServiceUrl))
                                .build();
        }

    private boolean esRutaReservas(String path) {
        return path.equals("/api/v1/reservas") || path.startsWith("/api/v1/reservas/")
                || path.equals("/api/v1/solicitudes") || path.startsWith("/api/v1/solicitudes/")
                || path.equals("/api/v1/agenda") || path.startsWith("/api/v1/agenda/")
                || path.equals("/api/v1/disponibilidad") || path.startsWith("/api/v1/disponibilidad/");
    }

        private boolean esRutaAcademica(String path) {
                return path.equals("/api/v1/campus") || path.startsWith("/api/v1/campus/")
                                || path.equals("/api/v1/bloques") || path.startsWith("/api/v1/bloques/")
                                || path.equals("/api/v1/pisos") || path.startsWith("/api/v1/pisos/")
                                || path.equals("/api/v1/laboratorios") || path.startsWith("/api/v1/laboratorios/")
                                || path.equals("/api/v1/equipos") || path.startsWith("/api/v1/equipos/")
                                || path.equals("/api/v1/tipos-equipo") || path.startsWith("/api/v1/tipos-equipo/")
                                || path.equals("/api/v1/facultades") || path.startsWith("/api/v1/facultades/")
                                || path.equals("/api/v1/carreras") || path.startsWith("/api/v1/carreras/")
                                || path.equals("/api/v1/materias") || path.startsWith("/api/v1/materias/")
                                || path.equals("/api/v1/periodos-lectivos") || path.startsWith("/api/v1/periodos-lectivos/")
                                || path.equals("/api/v1/horarios") || path.startsWith("/api/v1/horarios/");
        }
}
