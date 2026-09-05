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
- Criterio de error HTTP: porcentaje de respuestas HTTP 5xx menor que 1 %.
- Repeticiones previstas: 10; repeticiones ejecutadas: 0.
- Estado actual: **NO EJECUTADA / NO CONCLUYENTE**.

```powershell
python -m locust -f tests/load/locustfile.py --headless --users 50 --spawn-rate 10 --run-time 1h --csv evidencia/fiabilidad-rNN --html evidencia/fiabilidad-rNN.html
```

`NN` se reemplaza por la repetición `01` a `10`. No se debe cambiar el host ni
la carga entre repeticiones comparables.

La campaña exige diez ejecuciones independientes de una hora, es decir, al
menos diez horas efectivas de observación continua, además de preparación del
entorno, monitoreo, recolección y validación de evidencia. No se completó dentro
de la ventana disponible de la entrega. Por integridad experimental no se usan
repeticiones parciales ni resultados del escenario de cinco minutos como
sustitutos de la campaña de fiabilidad.

El porcentaje HTTP 5xx mide respuestas fallidas respecto de solicitudes y no
equivale a disponibilidad temporal. El objetivo de disponibilidad de 99,5 %
requiere definir y observar tiempo apto frente a tiempo total durante las diez
ventanas completas. Hasta contar con esa evidencia, tanto la fiabilidad temporal
como la disponibilidad permanecen **NO CONCLUYENTES**; un valor 5xx menor que
1 % por sí solo no demuestra disponibilidad mayor o igual que 99,5 %.

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
de p95 es menor que 500 ms. El analizador puede decidir únicamente el criterio
acotado de tasa HTTP 5xx cuando el límite superior de su IC 95 % es menor que
1 %; esa decisión no se etiqueta como disponibilidad. El script
`analizar_iso25010.py` aplica estas reglas y no calcula resultados con muestras
incompletas.

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

## Pre-registro ARBITER S0--S4

### Estado y aislamiento

Este apartado pre-registra el experimento exigido por la guía FUVV, secciones
5.1--5.6. A la fecha existen **0 de 130 corridas ejecutadas**. No contiene
resultados. El producto reserva laboratorios; como el experimento requiere un
equipo concreto, se utiliza un estado experimental separado con `equipmentId`,
franja, agente, estado, versión, `runId` y marcas temporales. No se modifica la
API ni la persistencia productiva.

El subsistema solo se habilita con `EXPERIMENTAL_ARBITER_ENABLED=true`, un valor
válido de `ARBITER` y la clave interna. Sin esas tres condiciones no se inicia.
`ARBITER` ausente no selecciona ninguna estrategia y el flujo productivo sigue
usando su implementación actual (`SERIALIZABLE`, reintentos, idempotencia,
mutex de agenda y validación de disponibilidad). S0 nunca es predeterminado.

### Tratamientos predefinidos

- **S0, escritura directa sin arbitraje:** control negativo exclusivamente
  experimental; no hace atómica la decisión y permite confirmaciones solapadas.
- **S1, bloqueo optimista:** lee versión y confirma mediante compare-and-set;
  el perdedor observa la versión modificada.
- **S2, bloqueo pesimista:** bloquea `equipmentId + inicio + fin` antes de
  comprobar conflictos y decidir.
- **S3, coordinador elegido:** tres nodos lógicos, algoritmo Bully (mayor ID
  vivo), heartbeat/detección en el backend, reloj Lamport y sección serial del
  líder. Lamport/Bully se reconstruyeron para E4 porque el artefacto E1 citado
  por la guía no está presente en HEAD.
- **S4, serializable por quórum:** transacción `SERIALIZABLE` y reintento por
  `SerializationFailure` en CockroachDB real. Su liderazgo pertenece al
  consenso del clúster; no es coordinador de aplicación ni usa Bully.

El almacenamiento SQL es administrado por `reservas-solicitudes-service` y,
solo con el modo experimental habilitado, crea el esquema `scli_experimental`;
los tests usan dobles en memoria. El harness nunca escribe directamente en
CockroachDB. Los equipos pueden
leerse de Académico. `fixtures/equipos-experimentales.json` es una fixture
sintética versionada y nunca representa inventario institucional.

### Matriz oficial

| Escenario | Configuraciones | Repeticiones | Corridas |
|---|---:|---:|---:|
| Esc-1, nominal: 50 usuarios/5 min | productiva | 10 | 10 |
| Esc-2: 50 simultáneas, mismo equipo/franja | S0--S4 | 10 | 50 |
| Esc-3: 200 simultáneas, mismo equipo/franja | S0--S4 | 10 | 50 |
| Esc-4: 200 simultáneas, caída al 50 % | S3 y S4 | 10 | 20 |
| **Total** | | | **130** |

Se conserva toda repetición. Las repeticiones 1 y 10 se excluyen del análisis;
2--9 forman ocho muestras candidatas. Se alternará el orden de estrategias
entre bloques y se mantendrá la semilla registrada. S0+Esc-3 debe exhibir doble
adjudicación; si no ocurre, se invalida el bloque comparativo y se revisan la
barrera y la posible presencia accidental de arbitraje.

### Generación, caída y recuperación

`generador_rafagas.py` usa un `Barrier` para agentes y coordinador, y conserva
`run_id`, SHA, semilla, tiempos de preparación/envío/respuesta y latencia. No se
ejecutan ráfagas en el PC. En Esc-4, S3 desactiva el líder Bully al 50 % y
registra la nueva elección. Para S4, `caida_coordinador.py` exige confirmación y
un comando reversible específico de la VM para detener un nodo Cockroach sin
borrar datos ni volúmenes. Se mide separadamente recuperación del líder de
aplicación S3 y recuperación del servicio respaldado por consenso S4.

### Oráculo y variables respuesta

El oráculo verifica: (1) ausencia de confirmaciones solapadas por equipo; (2)
exactamente un adjudicatario por confirmación; (3) ausencia de adjudicación
sobre equipo en mantenimiento; (4) exactamente un `RELEASED` por `CANCELLED`;
y (5) ningún `ACCESS_GRANTED` sin adjudicación vigente del mismo usuario,
equipo y franja. Sin eventos de cancelación o acceso, esos invariantes se
informan `NOT_OBSERVED`, no `PASS`. El quinto valida la regla experimental, no
una integración con hardware universitario.

La tasa principal es la proporción de franjas con más de un usuario confirmado,
con intervalo binomial Wilson 95 %. Un rechazo es innecesario cuando no existe
confirmación incompatible que lo justifique. Para Jain, `xi` es el número de
adjudicaciones confirmadas de cada agente; se informa además el vector por
equipo. El tiempo de recuperación va desde la marca de fallo hasta la vuelta
funcional y conserva ambos timestamps.

Por métrica se calcularán media, desviación muestral, IC95 y mediana. Las
comparaciones predefinidas son S1--S4 contra S0 dentro del mismo escenario y,
además, S1 vs S2, S2 vs S4 y S3 vs S4. Se usa Mann--Whitney bilateral y A12 con
grupo A igual a la primera estrategia nombrada. A12 representa
`P(A>B)+0.5P(A=B)`. Menor es favorable para dobles, rechazos, latencia y
recuperación; mayor es favorable para Jain. Se usarán diagramas de caja.

### Amenazas pre-registradas

- Ruido del host y competencia: descarte analítico de primera/última repetición,
  registro de entorno y alternancia del orden.
- Sincronía artificial de la barrera: aumenta deliberadamente la contención y
  no representa todas las llegadas reales.
- Orden de tratamientos: se alterna entre bloques.
- Cantidad sintética de equipos/laboratorios: limita generalización al campus.
- Demanda sintética frente a picos reales: limita validez externa.

La evidencia real irá a `resultados/arbiter/raw`, `oracle`, `summary` y
`analysis`. Solo después de ejecutar se generará un `SHA256SUMS` separado, sin
alterar el manifiesto histórico ISO 25010.

### Preparación reproducible (no ejecutar en PC)

En la VM autorizada se fija el SHA desplegado y se exportan, sin registrarlas,
la clave y URL internas del servicio. S4 utiliza el datasource CockroachDB real
configurado en `reservas-solicitudes-service`:

```text
EXPERIMENTAL_ARBITER_ENABLED=true
ARBITER=s4
INTERNAL_API_KEY=<secreto de la VM>
RESERVAS_EXPERIMENTAL_URL=http://<reservas-interno>/api/v1/internal/experimentos/arbiter/adjudicar
```

El plan determinista se materializa con
`python experimentos/planificar_arbiter.py --output <ruta>/plan.json`. Cada
corrida Esc-2/3 se invoca con `generador_rafagas.py --strategy <sN>
--scenario <esc2|esc3> --rep <1..10> --equipment-id <id> --laboratory-id <id>
--starts-at <ISO-8601> --ends-at <ISO-8601>`. Esc-4/S4 añade el comando
reversible y el health interno; exige el literal de confirmación
`CONFIRM_EXPERIMENTAL_NODE_FAILURE`. El oráculo se ejecuta sobre cada manifiesto
y la fixture/catálogo declarado. Ningún comando contiene credenciales en sus
argumentos ni ejecuta las 130 corridas automáticamente.
