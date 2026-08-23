package ec.edu.scli.academico.infrastructure.observability;

import ec.edu.scli.academico.infrastructure.observability.dto.PrometheusRangeResponse;
import ec.edu.scli.academico.infrastructure.observability.dto.PrometheusSerieCruda;
import ec.edu.scli.academico.presentation.dto.laboratorio.PuntoSerieResponse;
import ec.edu.scli.academico.presentation.dto.laboratorio.SerieEstadoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Cliente que consulta a Prometheus (PromQL, vía su API HTTP /api/v1/query_range)
 * el histórico de la métrica laboratorios_por_estado, y lo traduce a DTOs
 * limpios para el frontend. Es la única clase del servicio que conoce el
 * formato de respuesta propio de Prometheus.
 */
@Component
public class PrometheusQueryClient {

    private final RestClient restClient;

    public PrometheusQueryClient(@Value("${prometheus.url}") String prometheusUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(prometheusUrl)
                .build();
    }

    public List<SerieEstadoResponse> consultarOcupacionHistorica(int rangoMinutos) {
        OffsetDateTime fin = OffsetDateTime.now();
        OffsetDateTime inicio = fin.minusMinutes(rangoMinutos);

        PrometheusRangeResponse respuesta = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/query_range")
                        .queryParam("query", "laboratorios_por_estado")
                        .queryParam("start", inicio.toEpochSecond())
                        .queryParam("end", fin.toEpochSecond())
                        .queryParam("step", "30s")
                        .build())
                .retrieve()
                .body(PrometheusRangeResponse.class);

        if (respuesta == null || !"success".equals(respuesta.status())) {
            throw new IllegalStateException(
                    "Prometheus no respondio correctamente a la consulta de ocupacion historica");
        }

        return respuesta.data().result().stream()
                .map(this::aSerieEstado)
                .toList();
    }

    private SerieEstadoResponse aSerieEstado(PrometheusSerieCruda serieCruda) {
        String estado = serieCruda.metric().getOrDefault("estado", "DESCONOCIDO");

        List<PuntoSerieResponse> puntos = serieCruda.values().stream()
                .map(this::aPunto)
                .toList();

        return new SerieEstadoResponse(estado, puntos);
    }

    private PuntoSerieResponse aPunto(List<Object> valorPrometheus) {
        long epochSegundos = ((Number) valorPrometheus.get(0)).longValue();
        double valor = Double.parseDouble((String) valorPrometheus.get(1));

        OffsetDateTime instante = OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(epochSegundos), ZoneOffset.UTC);

        return new PuntoSerieResponse(instante, valor);
    }
}