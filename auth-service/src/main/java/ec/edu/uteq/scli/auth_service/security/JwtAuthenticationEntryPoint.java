package ec.edu.uteq.scli.auth_service.security;

import ec.edu.uteq.scli.auth_service.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class JwtAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException) throws IOException {

        ErrorResponse errorResponse = new ErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Se requiere un token de acceso válido",
                request.getRequestURI());

        response.setStatus(
                HttpStatus.UNAUTHORIZED.value());

        response.setContentType(
                "application/json");

        response.setCharacterEncoding(
                "UTF-8");

        objectMapper.writeValue(
                response.getOutputStream(),
                errorResponse);
    }
}