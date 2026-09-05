# Resumen verificable ISO 25010 — Entrega 4

## Proveniencia

- Host de ejecución: `servidor-proyectos`.
- Repositorio de ejecución: `/home/ffarinangog2/proyectos/miscli`.
- Git SHA probado: `a47f0441f644bea5f52944b7a11216f37b2242de`.
- Locust: 2.31.6, Python 3.11.10.
- Evidencia recuperada sin modificar los originales de la VM.
- Integridad: `SHA256SUMS` contiene los hashes de los archivos seleccionados.

Los reportes HTML y los CSV `locust_stats_history.csv` completos permanecen en la
VM. No se versionan porque son derivados voluminosos; se preservan aquí los CSV
agregados canónicos, fallos, excepciones, logs, metadata, consultas/resultados
Prometheus, salud del entorno y estadísticas de contenedores.

## Rampa exploratoria 0 → 200 usuarios, 10 minutos

- Directorio: `raw/ramp_0_200_10m/run-20260830T041941Z/`.
- Inicio UTC: `2026-08-30T04:19:41Z`.
- Fin UTC: `2026-08-30T04:30:01Z`.
- Solicitudes canónicas: 29.217.
- HTTP 500: 1 (`GET /api/v1/reservas`).
- Tasa: `1 / 29.217 × 100 = 0,00342266 %`.
- p95 Locust: 180 ms.
- p99 Locust: 1.100 ms.
- Máximo Locust: 14.024,163663 ms.
- Estado registrado: `failed`, coherente con `--exit-code-on-error 1` y el HTTP 500.

La rampa es exploratoria y no forma parte de las ocho muestras del CSV ISO central.

## Eficiencia nominal, 50 usuarios, 5 minutos

Las diez repeticiones finalizaron con `exit_code=0`, pertenecen al SHA indicado y
registran 0 fallos HTTP. El análisis estadístico usa exclusivamente r2–r9; r1 y r10
se conservan, pero se excluyen según el protocolo.

Resultados calculados por `experimentos/analizar_iso25010.py`:

| Métrica | n | Media | s muestral | IC95 | Decisión |
| --- | ---: | ---: | ---: | --- | --- |
| HTTP 5xx | 8 | 0 % | 0 % | [0; 0] % | CUMPLE `<1 %` |
| p95 Locust | 8 | 57,500000 ms | 37,132966 ms | [26,456064; 88,543936] ms | CUMPLE `<500 ms` |
| p99 Locust | 8 | 624,000000 ms | 606,585526 ms | [116,881810; 1.131,118190] ms | NO CUMPLE `<750 ms` |

El cálculo usa `df=7` y `t(0,975;7)=2,364624251`.

Como contraste, los resultados Prometheus p95 de r2–r9 producen:

- media: 18,283375 ms;
- desviación estándar muestral: 8,240924 ms;
- IC95: [11,393791; 25,172960] ms;
- HTTP 5xx en las diez ventanas: 0.

Locust y Prometheus miden en puntos distintos del sistema y no deben presentarse
como métricas intercambiables.

## Fiabilidad

**NO EJECUTADA — 0/10.**

No existen repeticiones de una hora. El protocolo se detuvo antes de fiabilidad por
el HTTP 500 de la rampa y la elevada variabilidad de p99, cuyo límite superior del
IC95 supera 750 ms. No se afirma disponibilidad de 99,5 %.
