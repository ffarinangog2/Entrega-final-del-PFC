package ec.edu.scli.reservas.config;

import ec.edu.scli.reservas.infrastructure.audit.AuditLogger;
import ec.edu.scli.reservas.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Seguridad HTTP stateless para la API de Reservas y Solicitudes. */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuditLogger auditLogger) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "Se requiere un token Bearer válido")))
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, exception) -> {
                            auditLogger.registrarEvento(
                                    "acceso_denegado",
                                    obtenerUsuario(),
                                    obtenerIp(request),
                                    request.getMethod() + " " + request.getRequestURI());
                            response.sendError(
                                    HttpServletResponse.SC_FORBIDDEN,
                                    "No tiene permisos para acceder al recurso");
                        }))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/solicitudes").hasAuthority("SOLICITUD_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/solicitudes/**").hasAuthority("SOLICITUD_CREAR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/solicitudes/*/revision").hasAuthority("SOLICITUD_APROBAR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/solicitudes/*/aprobar").hasAuthority("SOLICITUD_APROBAR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/solicitudes/*/rechazar").hasAuthority("SOLICITUD_RECHAZAR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/solicitudes/*/propuesta").hasAuthority("SOLICITUD_APROBAR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/solicitudes/*/propuesta/aceptar").hasAuthority("SOLICITUD_CREAR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/solicitudes/*/propuesta/rechazar").hasAuthority("SOLICITUD_CREAR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/solicitudes/*/cancelar").hasAuthority("SOLICITUD_CANCELAR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/solicitudes/**").hasAuthority("SOLICITUD_LEER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/reservas/**").hasAuthority("RESERVA_LEER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/reservas/*/cancelar").hasAuthority("RESERVA_CANCELAR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/reservas/**").hasAuthority("AGENDA_GESTIONAR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/disponibilidad/**").hasAuthority("LABORATORIO_LEER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/agenda/**").hasAnyAuthority("RESERVA_LEER", "AGENDA_GESTIONAR")
                        .requestMatchers("/api/v1/agenda/bloqueos/**").hasAuthority("AGENDA_GESTIONAR")
                        .anyRequest().authenticated())
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private String obtenerUsuario() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "desconocido";
    }

    private String obtenerIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor != null ? forwardedFor : request.getRemoteAddr();
    }
}
