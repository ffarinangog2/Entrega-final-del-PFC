package ec.edu.scli.academico.infrastructure.observability.dto;

import java.util.List;

public record PrometheusRangeData(

        String resultType,

        List<PrometheusSerieCruda> result

) {
}