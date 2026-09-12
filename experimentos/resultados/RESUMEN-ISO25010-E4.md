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

## Fiabilidad nominal, 50 usuarios, 1 hora

Las diez repeticiones oficiales finalizaron y son válidas. Se ejecutaron sobre
`feature/entrega-4`, Git SHA
`061a1050a94e1bd30d81b30c47c7e818005a33bb`, con Locust 2.31.6 y Python 3.12.3.
Todas conservaron `status=completed`, `duration_completed=true`,
`environment_consistent=true`, `evidence_complete=true` y
`execution_completed=true`. Cada ventana duró aproximadamente 3.600 segundos.

El código real de salida de Locust fue 1 en las diez repeticiones y se conserva en
`metadata.json`. Esto no invalida las ventanas: completaron la hora y los fallos HTTP
son resultados reales. El análisis estadístico usa exclusivamente r2–r9; r1 y r10
se conservan, pero se excluyen según el protocolo.

Resultados reproducibles de `python3 experimentos/analizar_iso25010.py
experimentos/resultados/iso25010.csv`:

| Métrica | n | Media | s muestral | IC95 | Decisión |
| --- | ---: | ---: | ---: | --- | --- |
| Tasa HTTP 5xx | 8 | 0,061315 % | 0,035011 % | [0,032045; 0,090585] % | CUMPLE `<1 %` |
| p95 Locust | 8 | 28,750000 ms | 3,150964 ms | [26,115729; 31,384271] ms | CUMPLE `<500 ms` |
| p99 Locust | 8 | 115,625000 ms | 26,521891 ms | [93,452144; 137,797856] ms | CUMPLE `<750 ms` |

El cálculo usa `df=7` y `t(0,975;7)=2,364624251`. La tasa del CSV es
`100 × HTTP 5xx / total_requests`; el conteo 5xx es la suma exacta de
`Occurrences` con estado 5xx en `locust_failures.csv`. Los percentiles y el total
proceden de la fila `Aggregated` de `locust_stats.csv`.

Locust también registró entre 66.476 y 67.008 respuestas HTTP 401 por repetición.
Estos fallos masivos no se ocultan: están conservados en `locust_failures.csv` y su
conteo consta en `observacion` de cada fila. El criterio preregistrado de fiabilidad
mide exclusivamente HTTP 5xx, por lo que los 401 no se suman a `failures`.

En r1, Locust observó 49 respuestas HTTP 500, mientras que la consulta
`increase()` de Prometheus produjo aproximadamente 4,0093 y un porcentaje de
0,016860 %. La discrepancia se conserva explícitamente en el CSV y en los
artefactos raw; no se corrigió ni sustituyó ninguna medición. En r2–r10, el conteo
entero de Locust coincide estrechamente con el resultado fraccional de Prometheus.
Los porcentajes de Prometheus usan el denominador observado por el servicio y no
son intercambiables con la tasa calculada sobre las solicitudes de Locust.

Este resultado permite decidir el criterio acotado de tasa HTTP 5xx. No demuestra
por sí solo una disponibilidad temporal mayor o igual que 99,5 %.

## Consolidación oficial E3: seguridad, mantenibilidad y compatibilidad

Las tres campañas se ejecutaron sobre el software del SHA
`fa7d75ec0f75573938bf46ed6a68f0aee99606ac`. El HEAD documental posterior
incorpora análisis y documentación y no se presenta como el software medido.
La fuente consolidada inalterada es [`analisis-e3.json`](analisis-e3.json).

### Seguridad

| Repeticiones | Correctas | Proporción | IC95 Wilson | Falsos permitidos | Falsos rechazados | Flaky | Decisión |
|---:|---:|---:|---|---:|---:|---:|---|
| 3 | 21/21 | 1,0 | [0,845360981013798; 1,0] | 0 | 0 | 0 | **CUMPLE** |

La decisión se limita a las siete decisiones dinámicas por repetición, fixtures,
Gateway y entorno ensayados. No constituye una garantía universal de seguridad.

### Mantenibilidad

| Componente | Métrica | Media/IC95 | Umbral | Decisión |
|---|---|---:|---:|---|
| Auth | Líneas | 88,042203985932 % | 70 % | CUMPLE |
| Usuarios | Líneas | 84,0523509452254 % | 70 % | CUMPLE |
| Académico | Líneas | 83,16089903674634 % | 70 % | CUMPLE |
| Reservas | Líneas | 84,35857805255023 % | 80 % | CUMPLE |
| Reservas | Ramas | 56,72559569561876 % | 48 % | CUMPLE |
| Gateway | Líneas | 88,88888888888889 % | 70 % | CUMPLE |
| Web | Líneas | 89,91 % | 70 % | CUMPLE |
| Web | Ramas | 73,69 % | 70 % | CUMPLE |
| Web | Funciones | 81,87 % | 70 % | CUMPLE |
| Web | Sentencias | 85,96 % | 70 % | CUMPLE |
| Android | Líneas | **38,34070796460177 %** | 70 % | **NO CUMPLE** |

Los tres valores de cada métrica fueron idénticos: `sample_sd = 0` y el IC95 t
es `[media; media]`. Esto describe repetición idéntica del proceso sobre el
mismo SHA, no certeza universal. La decisión global es **NO CUMPLE únicamente
por Android**.

### Compatibilidad

| Motor | Aprobados | Fallidos | Omitidos | Flaky | IC95 Wilson | Decisión |
|---|---:|---:|---:|---:|---|---|
| Chromium | 24/24 | 0 | 0 | 0 | [0,862023795269197; 1,0] | CUMPLE |
| Firefox | 24/24 | 0 | 0 | 0 | [0,862023795269197; 1,0] | CUMPLE |
| WebKit | 24/24 | 0 | 0 | 0 | [0,862023795269197; 1,0] | CUMPLE |

La decisión global es **CUMPLE** para la suite, motores y entorno ensayados; no
se extrapola a todos los navegadores, versiones o dispositivos.

El diseño y las reglas están en [`../protocolo-e4.md`](../protocolo-e4.md), y
la cadena requisito → protocolo → productor → raw → análisis → documento está
en [`TRAZABILIDAD-E3-E4.md`](TRAZABILIDAD-E3-E4.md). La selección compacta
[`evidencia-e3-canonica/`](evidencia-e3-canonica/) conserva manifiestos,
resultados consumidos por el analizador y reportes fuente de cobertura. El raw
E3 completo continúa disponible en el entorno de ejecución y no se versiona por
su volumen; la selección canónica no sustituye ni modifica esos originales.
