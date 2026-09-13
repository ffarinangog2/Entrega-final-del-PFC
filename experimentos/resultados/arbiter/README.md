# Evidencia ARBITER E4

Este directorio conserva la evidencia versionada de la campaña ARBITER ejecutada
sobre el SHA experimental
`e43967eda31410b7fef060cfc7903cea94efbe96`.

La campaña planificó **130 corridas**. El estado registrado se descompone así:

- **123 corridas COMPLETED**;
- **7 corridas FAILED**.

Por tanto, `130 planificadas = 123 completadas + 7 fallidas`.

De las 123 corridas completadas:

- **99** no fueron excluidas y se utilizaron en el análisis;
- **24** fueron excluidas del análisis.

La evidencia se encuentra en [`campaign/`](campaign/) y está organizada de la
siguiente manera:

- [`campaign/manifest/checkpoint.json`](campaign/manifest/checkpoint.json):
  manifiesto de las 130 corridas, su estado y su exclusión del análisis;
- [`campaign/summary/runs.json`](campaign/summary/runs.json) y
  [`campaign/summary/runs.csv`](campaign/summary/runs.csv): resumen estructurado
  de las corridas;
- [`campaign/analysis/comparisons.json`](campaign/analysis/comparisons.json):
  **24 comparaciones estadísticas**;
- [`campaign/analysis/oracle/`](campaign/analysis/oracle/): **120 archivos de
  oráculo**;
- [`campaign/raw/`](campaign/raw/): evidencia raw conservada por corrida;
- [`campaign/SHA256SUMS`](campaign/SHA256SUMS): integridad SHA-256 de los
  **315 archivos** incluidos en el manifiesto de hashes. La verificación se
  completa sin errores.

Los tests unitarios del harness validan el instrumental, pero no sustituyen la
evidencia experimental registrada en `campaign/`.

## Censo HTTP central r2--r9

El censo siguiente se deriva directamente de los registros `REQUEST` de
Esc-2/Esc-3. Todos los fallos observados fueron HTTP 500; no hubo respuestas
4xx.

| Escenario | Estrategia | Requests | 2xx | 4xx | 5xx | Error | Observaciones estadísticas |
|---|---|---:|---:|---:|---:|---:|---:|
| Esc-2 | S0 | 400 | 400 | 0 | 0 | 0 % | 8 |
| Esc-2 | S1 | 400 | 400 | 0 | 0 | 0 % | 8 |
| Esc-2 | S2 | 400 | 348 | 0 | 52 | 13,0000 % | 8 |
| Esc-2 | S3 | 400 | 400 | 0 | 0 | 0 % | 8 |
| Esc-2 | S4 | 400 | 396 | 0 | 4 | 1,0000 % | 8 |
| Esc-3 | S0 | 1.600 | 1.586 | 0 | 14 | 0,8750 % | 8 |
| Esc-3 | S1 | 1.600 | 1.173 | 0 | 427 | **26,6875 %** | 8 |
| Esc-3 | S2 | 1.600 | 1.381 | 0 | 219 | 13,6875 % | 8 |
| Esc-3 | S3 | 1.600 | 1.600 | 0 | 0 | 0 % | 8 |
| Esc-3 | S4 | 1.600 | 1.368 | 0 | 232 | 14,5000 % | 8 |

S0 es el baseline formal y control negativo; S3 no es el baseline. El contraste
descriptivo 26,6875 % frente a 0 % corresponde a S1 frente a S3 en Esc-3. Frente
al baseline formal S0, es 26,6875 % frente a 0,8750 %.

Cada `HTTP_ERROR` preserva `sent_ns`, `received_ns` y `latency_ms`, y participa
en el promedio de latencia de su corrida. La población principal representa así
todos los intentos bajo carga, incluidos los HTTP 500; no representa únicamente
adjudicaciones exitosas.

## Alcance de la interpretación

Las comparaciones de doble adjudicación conservan `U = 64`,
`p = 0,0007775304469403844`, `A12 = 1,0` y `n = 8` por grupo. Son
matemáticamente reproducibles y los 500 no invalidan su cálculo, pero no prueban
disponibilidad equivalente. `HTTP_ERROR` no se convierte en confirmación ni en
rechazo: cero dobles adjudicaciones o cero rechazos innecesarios pueden coexistir
con fallos HTTP. El resultado acredita seguridad observada bajo la carga
ofrecida, no superioridad operacional global.

Restringir ahora la población principal a respuestas 2xx sería una redefinición
post hoc no prerregistrada. Un análisis solo-2xx, si se realizara, sería una
sensibilidad secundaria y no sustituiría el resultado principal.

El censo puede reproducirse sin escribir resultados mediante:

```text
python experimentos/auditar_http_arbiter.py experimentos/resultados/arbiter/campaign/raw
```
