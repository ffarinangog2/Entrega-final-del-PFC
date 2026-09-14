package ec.edu.uteq.scli.auth_service.infrastructure.config;

import ec.edu.uteq.scli.auth_service.infrastructure.security.CustomUserDetailsService;
import ec.edu.uteq.scli.auth_service.infrastructure.security.JwtAuthenticationEntryPoint;
import ec.edu.uteq.scli.auth_service.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }

        @Bean
        public DaoAuthenticationProvider authenticationProvider(
                        CustomUserDetailsService customUserDetailsService,
                        PasswordEncoder passwordEncoder) {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider(
                                customUserDetailsService);

                provider.setPasswordEncoder(passwordEncoder);

                return provider;
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration configuration) throws Exception {
                return configuration.getAuthenticationManager();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        DaoAuthenticationProvider authenticationProvider,
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) throws Exception {

                http
                                .csrf(csrf -> csrf.disable())

                                .formLogin(form -> form.disable())

                                .httpBasic(basic -> basic.disable())

                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))

                                .authenticationProvider(
                                                authenticationProvider)

                                .exceptionHandling(exception -> exception.authenticationEntryPoint(
                                                jwtAuthenticationEntryPoint))

                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/actuator/health",
                                                                "/actuator/info",
                                                                "/actuator/prometheus",

                                                                "/api/v1/auth/login",
                                                                "/api/v1/auth/refresh",
                                                                "/api/v1/auth/logout",
                                                                "/api/v1/auth/forgot-password",
                                                                "/api/v1/auth/reset-password",

                                                                "/api/v1/internal/**",

                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()

                                                .requestMatchers("/api/v1/auth/admin/**")
                                                .hasRole("ADMINISTRADOR")

                                                .anyRequest()
                                                .authenticated())

                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
