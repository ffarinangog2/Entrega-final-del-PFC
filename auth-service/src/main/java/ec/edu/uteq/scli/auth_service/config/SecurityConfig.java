package ec.edu.uteq.scli.auth_service.config;

import ec.edu.uteq.scli.auth_service.security.CustomUserDetailsService;
import ec.edu.uteq.scli.auth_service.security.JwtAuthenticationEntryPoint;
import ec.edu.uteq.scli.auth_service.security.JwtAuthenticationFilter;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
        public CorsConfigurationSource corsConfigurationSource() {

                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOrigins(
                                List.of("http://localhost:5173"));

                configuration.setAllowedMethods(
                                List.of(
                                                "GET",
                                                "POST",
                                                "PUT",
                                                "PATCH",
                                                "DELETE",
                                                "OPTIONS"));

                configuration.setAllowedHeaders(
                                List.of(
                                                "Authorization",
                                                "Content-Type",
                                                "Accept"));

                configuration.setExposedHeaders(
                                List.of("Authorization"));

                configuration.setAllowCredentials(true);

                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

                source.registerCorsConfiguration(
                                "/**",
                                configuration);

                return source;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        DaoAuthenticationProvider authenticationProvider,
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                        CorsConfigurationSource corsConfigurationSource) throws Exception {

                http
                                .cors(cors -> cors.configurationSource(
                                                corsConfigurationSource))

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

                                                                "/api/v1/auth/login",
                                                                "/api/v1/auth/refresh",

                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()

                                                .anyRequest()
                                                .authenticated())

                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}