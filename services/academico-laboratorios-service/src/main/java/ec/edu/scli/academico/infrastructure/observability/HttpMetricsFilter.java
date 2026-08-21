package ec.edu.scli.academico.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Filtro HTTP que incrementa la métrica http_requests_total en cada
 * petición procesada por academico-laboratorios-service, etiquetada
 * por ruta, método HTTP y código de estado de la respuesta.
 */
@Component
public class HttpMetricsFilter extends HttpFilter {
    private static final Logger log = LoggerFactory.getLogger(HttpMetricsFilter.class);

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response,
                             FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            String ruta = obtenerRutaPlantilla(request);
            String metodo = request.getMethod();
            String status = String.valueOf(response.getStatus());

            HttpRequestsMetricsRegistry.getInstance()
                .incrementarPeticion(ruta, metodo, status);
            log.info("Peticion procesada: {} {} -> {}", metodo, ruta, status);
        }
    }

    private String obtenerRutaPlantilla(HttpServletRequest request) {
        Object patron = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return patron != null ? patron.toString() : request.getRequestURI();
    }
}