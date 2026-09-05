# Ejecución reproducible ISO 25010 de Freddy

## Ejecutar una repetición

Eficiencia, 50 usuarios durante 5 minutos:

```powershell
powershell -ExecutionPolicy Bypass -File experimentos/ejecutar_iso25010.ps1 -Escenario eficiencia_nominal_50u_5m -Repeticion 1 -HostObjetivo http://localhost:8080
```

Fiabilidad, 50 usuarios durante 1 hora:

```powershell
powershell -ExecutionPolicy Bypass -File experimentos/ejecutar_iso25010.ps1 -Escenario fiabilidad_nominal_50u_1h -Repeticion 1 -HostObjetivo http://localhost:8080
```

El host es obligatorio y configurable. Para la prueba integrada local debe ser el API
Gateway en `http://localhost:8080`. La herramienta no inicia servicios. Antes de una
ejecución real se debe instalar `tests/load/requirements.txt` y verificar el ambiente.

## Evidencia generada

Cada repetición real usa:

```text
experimentos/resultados/raw/<escenario>/rep-NN/
```

Locust genera `locust_stats.csv`, historial, fallos, excepciones, HTML y log. El archivo
`metadata.json` registra escenario, repetición, host, usuarios, duración, versión Locust,
comando y tiempos UTC. También se guardan las consultas Prometheus de p95, conteo 5xx
y porcentaje 5xx.

`raw/.gitignore` permite exclusivamente los artefactos canónicos seleccionados de
Entrega 4. Los HTML y los historiales completos de Locust permanecen en la VM porque
son derivados voluminosos. La procedencia, resultados y alcance de la selección se
documentan en `resultados/RESUMEN-ISO25010-E4.md`; `resultados/SHA256SUMS` permite
verificar su integridad.

## Obtener métricas reales

El total de requests se toma de la fila agregada de `locust_stats.csv`. La p95 se toma
de la columna de percentil 95 % del mismo agregado y se contrasta con
`prometheus-p95.promql`.

Los fallos de Locust no se copian automáticamente a `failures`: pueden incluir errores
de contenido o conectividad. Para Freddy, `failures` significa exclusivamente respuestas
HTTP 5xx. Al finalizar la repetición se ejecuta en Prometheus la consulta guardada en
`prometheus-5xx-count.promql`, usando como instante de evaluación el fin UTC registrado.
Su respuesta se conserva como `prometheus-5xx-result.txt`.

El porcentaje 5xx se obtiene con la consulta exacta guardada por la herramienta:

```promql
100 * sum(increase(http_server_requests_seconds_count{job="reservas-solicitudes-service",status=~"5.."}[DURACION])) / clamp_min(sum(increase(http_server_requests_seconds_count{job="reservas-solicitudes-service"}[DURACION])), 1)
```

`DURACION` es `5m` o `1h` según el escenario. No se debe sustituir este porcentaje por
el failure rate general de Locust.

## Registrar una fila verificada

Después de conservar las evidencias reales:

```powershell
python experimentos/registrar_iso25010.py --scenario eficiencia_nominal_50u_5m --repetition 2 --total-requests <TOTAL_REAL> --http-5xx <CONTEO_5XX_REAL> --p95-ms <P95_REAL> --evidence-dir experimentos/resultados/raw/eficiencia_nominal_50u_5m/rep-02
```

El importador calcula `failure_rate_percent`, exige ejecución Locust completada, CSV de
estadísticas y resultado Prometheus, y rechaza sobrescribir filas. Solo entonces marca
`valida=si`. Las repeticiones 1 y 10 pueden registrarse y conservarse, pero
`analizar_iso25010.py` siempre las excluye del análisis estadístico.
