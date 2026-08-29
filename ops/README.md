# Observabilidad reproducible

El stack local conecta los cinco servicios Spring con OpenTelemetry Collector mediante OTLP/HTTP. El Collector envía trazas a Tempo, logs a Loki y métricas OTLP a su exporter Prometheus. Prometheus también consulta directamente Actuator, CockroachDB y cAdvisor. Grafana provisiona automáticamente Prometheus, Loki, Tempo y el dashboard `pfc-dashboard.json`.

## Inicio y comprobación

Con un `.env` local completo y no versionado:

```bash
docker compose config --quiet
docker compose up -d
docker compose ps
```

- Grafana: `http://localhost:3001`.
- Prometheus: `http://localhost:9090`.
- Targets Prometheus: `http://localhost:9090/targets`.
- Tempo y Loki se consultan desde **Explore** en Grafana.
- Dashboard: carpeta **SCLI**, título **PFC - Observabilidad consolidada**.

Para generar tráfico, consulte el health del Gateway y ejecute un flujo funcional sin imprimir credenciales:

```bash
curl -fsS http://localhost:8080/actuator/health
```

Los servicios emiten JSON a consola y OTLP. Durante una petición instrumentada, `trace_id` y `span_id` permiten correlacionar los registros en Loki con la traza en Tempo. La instrumentación HTTP de Spring usa el contexto W3C `traceparent`. La verificación local confirmó la correlación log-traza en Gateway; la propagación completa Gateway → microservicio debe comprobarse con el stack funcional levantado, porque requiere ambos procesos.

## Paneles y señales

| Panel | Señal real |
| --- | --- |
| REQUEST RATE | `rate(http_server_requests_seconds_count[5m])` de Actuator/Micrometer |
| LATENCIA | cuantiles p50/p95/p99 de `http_server_requests_seconds_bucket` |
| ERRORES 5XX | tasa del contador HTTP filtrada por `status=~"5.."` |
| COCKROACHDB | `liveness_livenodes` y `ranges_unavailable` desde `/_status/vars` |
| RECURSOS | CPU y memoria de cAdvisor |
| LATENCIA E2E MÓVIL | proxy operacional p95 de rutas del Gateway usadas por Android |

El sexto panel no afirma distinguir Web de Android: si ambos consumen la misma ruta, la serie los agrega. Separarlos de forma rigurosa requeriría una dimensión de cliente controlada y de baja cardinalidad.

Las métricas estándar Micrometer `http_server_requests_seconds_count` y `http_server_requests_seconds_bucket` son los equivalentes operacionales de `request_count_total` y `request_duration_seconds`. Las métricas propias existentes son `app_business_events_total`, `app_active_sessions`, contadores de autenticación y `laboratorios_por_estado`.

## Producción

`docker-compose.prod.yml` incluye Collector, Tempo, Loki, Prometheus, Grafana y cAdvisor. Los servicios reciben endpoints OTLP resolubles dentro de `scli-network`; no apuntan a un collector inexistente. Prometheus y Grafana se enlazan únicamente a `127.0.0.1` para acceder mediante túnel SSH o proxy autenticado, y no se publican CockroachDB, Loki, Tempo, cAdvisor ni OTLP.

La contraseña de Grafana se obtiene de `GF_SECURITY_ADMIN_PASSWORD`; nunca debe almacenarse en Git. Grafana Cloud no está configurado: requeriría URL, tenant y token externos. El stack local no depende de esas credenciales.

## Validación de señales

1. En Prometheus, compruebe que los targets estén `UP`.
2. Genere una petición por Gateway.
3. Busque en Grafana Explore/Loki un registro del servicio y copie su `trace_id`.
4. Abra ese identificador en Tempo y confirme spans del Gateway y del servicio destino.
5. Abra el dashboard consolidado y verifique que las consultas produzcan series después de generar tráfico.

No se documentan cifras de disponibilidad o latencia como resultados: deben obtenerse al ejecutar los experimentos ISO 25010.
