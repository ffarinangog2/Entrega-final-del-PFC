package ec.edu.scli.reservas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Autenticación interna exclusiva del subsistema experimental ARBITER. */
@Component
public class ExperimentalInternalApiKeyFilter extends OncePerRequestFilter {

    public static final String AUTHORITY = "INTERNAL_ARBITER";
    static final String PATH_PREFIX = "/api/v1/internal/experimentos/arbiter/";
    private static final String HEADER = "X-Internal-Api-Key";

    private final boolean experimentalEnabled;
    private final byte[] expectedKey;

    public ExperimentalInternalApiKeyFilter(
            @Value("${app.experimental.arbiter.enabled:false}") boolean experimentalEnabled,
            @Value("${app.internal-api-key:}") String internalApiKey) {
        this.experimentalEnabled = experimentalEnabled;
        this.expectedKey = internalApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !experimentalEnabled || !request.getRequestURI().startsWith(PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String supplied = request.getHeader(HEADER);
        if (expectedKey.length == 0 || supplied == null || !MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.UTF_8), expectedKey)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "API key interna invalida");
            return;
        }
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "internal-arbiter", null, java.util.List.of(new SimpleGrantedAuthority(AUTHORITY)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
