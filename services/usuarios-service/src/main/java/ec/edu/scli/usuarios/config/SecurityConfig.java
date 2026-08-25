package ec.edu.scli.usuarios.config;

import ec.edu.scli.usuarios.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Seguridad HTTP stateless para la API de Usuarios. */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
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
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/api/v1/internal/perfiles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/perfiles/**").hasAuthority("USUARIO_LEER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/perfiles").hasAuthority("USUARIO_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/perfiles/**").hasAuthority("USUARIO_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/perfiles/*/estado").hasAuthority("USUARIO_DESACTIVAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/perfiles/**").hasAuthority("USUARIO_DESACTIVAR")
                        .anyRequest().authenticated())
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
