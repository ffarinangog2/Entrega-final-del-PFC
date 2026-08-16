package ec.edu.uteq.scli.api_gateway.routes;

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
}