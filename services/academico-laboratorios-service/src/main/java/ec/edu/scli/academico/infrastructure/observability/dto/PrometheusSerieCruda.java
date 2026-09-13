package ec.edu.scli.academico.infrastructure.observability.dto;

import java.util.List;
import java.util.Map;

/**
 * Una serie de Prometheus: sus etiquetas (ej. {"estado": "OCUPADO"}) y
 * su lista de puntos [timestamp_epoch, "valor_como_texto"].
 */
public record PrometheusSerieCruda(

        Map<String, String> metric,

        List<List<Object>> values

) {
}