package ec.edu.scli.academico.config;

import ec.edu.scli.academico.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
            HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED, "Se requiere un token Bearer válido"))
                        .accessDeniedHandler((request, response, exception) -> response.sendError(
                                HttpServletResponse.SC_FORBIDDEN, "No posee permisos para esta operación")))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/api/v1/internal/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/laboratorios/**")
                                .hasAnyAuthority("ACADEMICO_LEER", "LABORATORIO_LEER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/equipos/**", "/api/v1/tipos-equipo/**")
                                .hasAnyAuthority("ACADEMICO_LEER", "EQUIPO_LEER", "LABORATORIO_LEER")
                        .requestMatchers(HttpMethod.GET, ACADEMIC_PATHS).hasAuthority("ACADEMICO_LEER")
                        .requestMatchers("/api/v1/horarios/**", "/api/v1/periodos-lectivos/**")
                                .hasAnyAuthority("PLANIFICACION_GESTIONAR", "ROLE_ADMINISTRADOR")
                        .requestMatchers(ACADEMIC_PATHS).hasAuthority("ROLE_ADMINISTRADOR")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
