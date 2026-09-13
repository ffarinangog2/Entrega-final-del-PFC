# Observabilidad de Reservas/Solicitudes

Esta configuración agrega exclusivamente el scrape del servicio de Freddy en
`reservas-solicitudes-service:8084/actuator/prometheus`. No modifica ni levanta
Docker Compose, Prometheus o Grafana.

## Duración HTTP

Spring Boot y Micrometer generan el temporizador estándar `http.server.requests`,
exportado por el registry Prometheus como `http_server_requests_seconds`. La Guía de
Entrega 4 exige además el nombre literal `http_request_duration_seconds`. Las recording
rules de `rules/freddy-reservas.yml` crean ese alias a partir de los buckets, suma y
conteo estándar, sin instrumentar ni medir dos veces las solicitudes.

Los histogramas están habilitados en el `application.yml` del servicio. Sus buckets
`http_request_duration_seconds_bucket` permiten calcular percentiles agregables:

```promql
histogram_quantile(0.50, sum by (le) (rate(http_request_duration_seconds_bucket{job="reservas-solicitudes-service"}[5m])))
```

```promql
histogram_quantile(0.95, sum by (le) (rate(http_request_duration_seconds_bucket{job="reservas-solicitudes-service"}[5m])))
```

```promql
histogram_quantile(0.99, sum by (le) (rate(http_request_duration_seconds_bucket{job="reservas-solicitudes-service"}[5m])))
```

Porcentaje de respuestas HTTP 5xx:

```promql
100 * sum(rate(http_server_requests_seconds_count{job="reservas-solicitudes-service",status=~"5.."}[5m])) / clamp_min(sum(rate(http_server_requests_seconds_count{job="reservas-solicitudes-service"}[5m])), 1e-9)
```

Tasa de eventos de negocio agrupada por evento:

```promql
sum by (event) (rate(app_business_events_total{job="reservas-solicitudes-service"}[5m]))
```

Las etiquetas HTTP `uri` generadas por Spring usan las plantillas de ruta MVC, no UUID
concretos. El tablero de Freddy agrega a nivel de servicio y no introduce etiquetas de
alta cardinalidad.
