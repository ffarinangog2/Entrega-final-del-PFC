package ec.edu.scli.academico.infrastructure.observability.dto;

/** Respuesta cruda de Prometheus para una consulta /api/v1/query_range. */
public record PrometheusRangeResponse(

        String status,

        PrometheusRangeData data

) {
}