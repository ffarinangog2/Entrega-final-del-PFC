# Protocolo ISO 25010 de Freddy: Reservas/Solicitudes

## Objetivo

Evaluar la fiabilidad y la eficiencia de desempeño de las consultas HTTP de
`reservas-solicitudes-service` bajo carga nominal. Este documento define el diseño y
la recolección; no contiene resultados experimentales.

## Entorno que debe registrarse

Antes de cada lote se debe conservar:

- fecha y hora UTC, commit y estado del árbol Git;
- host objetivo y despliegue utilizado, sin publicar secretos;
- sistema operativo, CPU, memoria y límites de recursos;
- versiones de Java, Spring Boot, Locust, Prometheus y Grafana;
- número de instancias del servicio y su configuración relevante;
- topología y estado de CockroachDB, junto con la instantánea de datos;
- procesos externos o incidencias capaces de afectar la medición;
- comando exacto de carga y archivos de configuración utilizados.

El ambiente, los datos y la configuración deben permanecer constantes durante las diez
repeticiones equivalentes. Cualquier desviación se registra en `observacion`.

## Escenarios

### Eficiencia nominal

- 50 usuarios concurrentes.
- Duración: 5 minutos por repetición.
- Carga de solo lectura definida en `tests/load/locustfile.py`.
- Criterio: latencia p95 menor que 500 ms.

Comando base, con `LOCUST_HOST` configurado para el ambiente autorizado:

```powershell
python -m locust -f tests/load/locustfile.py --headless --users 50 --spawn-rate 10 --run-time 5m --csv evidencia/eficiencia-rNN --html evidencia/eficiencia-rNN.html
```

### Fiabilidad nominal

- 50 usuarios concurrentes.
- Duración: 1 hora por repetición.
- La misma carga de solo lectura y el mismo ritmo de incorporación de usuarios.
- Criterio: porcentaje de respuestas HTTP 5xx menor que 1 %.

```powershell
python -m locust -f tests/load/locustfile.py --headless --users 50 --spawn-rate 10 --run-time 1h --csv evidencia/fiabilidad-rNN --html evidencia/fiabilidad-rNN.html
```

`NN` se reemplaza por la repetición `01` a `10`. No se debe cambiar el host ni
la carga entre repeticiones comparables.

## Repetibilidad y validez

Cada escenario se ejecuta diez veces (`r = 10`) y se registra con una fila por
repetición en `resultados/iso25010.csv`.

- Las repeticiones 1 y 10 se conservan como evidencia, pero se excluyen del análisis.
- Las repeticiones 2 a 9 forman las ocho muestras candidatas.
- Una muestra es válida cuando completa la duración, conserva el ambiente previsto y
  dispone de métricas reales y evidencia trazable.
- Las ejecuciones interrumpidas o alteradas no se eliminan: se marcan como no válidas y
  se explica la causa.
- No se reemplazan datos ausentes por cero ni se estiman mediciones.

El análisis solo produce estadísticas cuando existen exactamente ocho muestras válidas
y completas para el escenario.

## Recolección de métricas

Locust conserva el total de peticiones, sus fallos, percentiles y reporte HTML. Para el
CSV de este protocolo, `failures` registra respuestas HTTP 5xx observadas; otros fallos
de Locust se documentan adicionalmente en `observacion` y en los archivos originales.

Prometheus permite obtener el porcentaje 5xx de la hora completa con:

```promql
100 * sum(increase(http_server_requests_seconds_count{job="reservas-solicitudes-service",status=~"5.."}[1h])) / clamp_min(sum(increase(http_server_requests_seconds_count{job="reservas-solicitudes-service"}[1h])), 1)
```

Para la p95 del escenario de cinco minutos se usan los buckets del alias requerido:

```promql
1000 * histogram_quantile(0.95, sum by (le) (increase(http_request_duration_seconds_bucket{job="reservas-solicitudes-service"}[5m])))
```

El resultado anterior está en milisegundos. Los valores de Prometheus deben tomarse
usando exactamente el intervalo UTC de cada repetición. La p95 de Locust sirve como
contraste; cualquier discrepancia se conserva y explica, no se corrige manualmente.

## Análisis estadístico

Para cada métrica y escenario, con las ocho observaciones válidas `x_i`:

- media: `x̄ = Σx_i / n`, con `n = 8`;
- desviación estándar muestral: `s = sqrt(Σ(x_i - x̄)² / (n - 1))`;
- IC 95 %: `x̄ ± t(0.975, 7) × s / sqrt(8)`, usando `t = 2.364624251`.

La decisión es conservadora: eficiencia cumple cuando el límite superior del IC 95 %
de p95 es menor que 500 ms; fiabilidad cumple cuando el límite superior del IC 95 %
del porcentaje 5xx es menor que 1 %. El script `analizar_iso25010.py` aplica estas
reglas y no calcula resultados con muestras incompletas.

## Evidencia que debe conservarse

Por cada repetición se deben guardar:

- CSV y HTML originales de Locust;
- instante UTC de inicio y fin y comando ejecutado;
- exportación o captura de las consultas Prometheus usadas;
- captura del dashboard Grafana correspondiente al intervalo;
- logs del servicio durante la ejecución;
- manifiesto del entorno y estado de CockroachDB;
- fila completa en `resultados/iso25010.csv` y explicación de anomalías.

Los artefactos deben usar nombres con escenario y número de repetición. La plantilla
versionada no constituye evidencia ni resultado.
