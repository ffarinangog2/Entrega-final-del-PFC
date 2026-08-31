package ec.edu.scli.academico.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.scli.academico.infrastructure.audit.AuditLogger;
import ec.edu.scli.academico.presentation.exception.ApiError;
import ec.edu.scli.academico.security.JwtAuthenticationFilter;
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

@Configuration
public class SecurityConfig {

    private static final String[] ACADEMIC_PATHS = {
            "/api/v1/campus/**", "/api/v1/bloques/**", "/api/v1/pisos/**",
            "/api/v1/laboratorios/**", "/api/v1/equipos/**", "/api/v1/tipos-equipo/**",
            "/api/v1/facultades/**", "/api/v1/carreras/**", "/api/v1/materias/**",
            "/api/v1/periodos-lectivos/**", "/api/v1/horarios/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
            AuditLogger auditLogger, ObjectMapper objectMapper) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> escribirError(
                                objectMapper, request, response, HttpServletResponse.SC_UNAUTHORIZED,
                                "UNAUTHORIZED", "Se requiere un token Bearer válido"))
                        .accessDeniedHandler((request, response, exception) -> {
                            auditLogger.registrarEvento(
                                    "acceso_denegado",
                                    obtenerUsuario(),
                                    obtenerIp(request),
                                    request.getMethod() + " " + request.getRequestURI());
                            escribirError(objectMapper, request, response, HttpServletResponse.SC_FORBIDDEN,
                                    "FORBIDDEN", "No posee permisos para esta operación");
                        }))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/api/v1/internal/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/laboratorios/**")
                                .hasAnyAuthority("ACADEMICO_LEER", "LABORATORIO_LEER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/equipos/**", "/api/v1/tipos-equipo/**")
                                .hasAnyAuthority("ACADEMICO_LEER", "EQUIPO_LEER", "LABORATORIO_LEER")
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/horarios/**", "/api/v1/periodos-lectivos/**")
                                .hasAnyAuthority(
                                        "ACADEMICO_LEER", "PLANIFICACION_GESTIONAR", "ROLE_ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/laboratorios/**")
                                .hasAnyAuthority("LABORATORIO_GESTIONAR", "ROLE_ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/laboratorios/**")
                                .hasAnyAuthority("LABORATORIO_GESTIONAR", "ROLE_ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/laboratorios/**")
                                .hasAnyAuthority("LABORATORIO_GESTIONAR", "ROLE_ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/equipos/**")
                                .hasAnyAuthority("EQUIPO_GESTIONAR", "ROLE_ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/equipos/**")
                                .hasAnyAuthority("EQUIPO_GESTIONAR", "ROLE_ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/equipos/**")
                                .hasAnyAuthority("EQUIPO_GESTIONAR", "ROLE_ADMINISTRADOR")
                        .requestMatchers("/api/v1/materias/**")
                                .hasAnyAuthority("PLANIFICACION_GESTIONAR", "ROLE_ADMINISTRADOR")
                        .requestMatchers("/api/v1/horarios/**", "/api/v1/periodos-lectivos/**")
                                .hasAnyAuthority("PLANIFICACION_GESTIONAR", "ROLE_ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, ACADEMIC_PATHS).hasAuthority("ACADEMICO_LEER")
                        .requestMatchers(ACADEMIC_PATHS).hasAuthority("ROLE_ADMINISTRADOR")
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
