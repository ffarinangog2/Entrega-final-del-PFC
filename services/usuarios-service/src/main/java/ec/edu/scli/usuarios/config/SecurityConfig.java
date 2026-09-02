package ec.edu.scli.usuarios.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.scli.usuarios.infrastructure.audit.AuditLogger;
import ec.edu.scli.usuarios.presentation.exception.ApiError;
import ec.edu.scli.usuarios.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.io.IOException;
import java.time.OffsetDateTime;

/** Seguridad HTTP stateless para la API de Usuarios. */
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
            AuditLogger auditLogger, ObjectMapper objectMapper) throws Exception {
        http.csrf(csrf -> csrf.disable()).formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> escribirError(
                                objectMapper, request, response, 401, "UNAUTHORIZED",
                                "Se requiere un token Bearer válido"))
                        .accessDeniedHandler((request, response, exception) -> {
                            auditLogger.registrarEvento("acceso_denegado", obtenerUsuario(), obtenerIp(request),
                                    request.getMethod() + " " + request.getRequestURI());
                            escribirError(objectMapper, request, response, 403, "FORBIDDEN",
                                    "No tiene permisos para acceder al recurso");
                        }))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/api/v1/internal/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/perfiles/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/perfiles/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/docentes/perfil/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/perfiles/**").hasAuthority("USUARIO_LEER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/perfiles").hasAuthority("USUARIO_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/perfiles/**").hasAuthority("USUARIO_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/perfiles/*/estado").hasAuthority("USUARIO_DESACTIVAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/perfiles/**").hasAuthority("USUARIO_DESACTIVAR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/docentes/**")
                                .hasAnyAuthority("USUARIO_LEER", "PLANIFICACION_GESTIONAR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/estudiantes/**",
                                "/api/v1/administradores/**").hasAuthority("USUARIO_LEER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/docentes", "/api/v1/estudiantes",
                                "/api/v1/administradores").hasAuthority("USUARIO_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/docentes/**", "/api/v1/estudiantes/**",
                                "/api/v1/administradores/**").hasAuthority("USUARIO_EDITAR")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void escribirError(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response,
                               int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), new ApiError(
                OffsetDateTime.now(), status, error, message, request.getRequestURI(), null));
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
